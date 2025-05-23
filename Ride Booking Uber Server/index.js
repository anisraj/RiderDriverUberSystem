const http = require('http');
const path = require('path');
var grpc = require('@grpc/grpc-js');
var protoLoader = require('@grpc/proto-loader');

var DRIVER_PROTO_PATH = __dirname+'/driver.proto';
var RIDER_PROTO_PATH = __dirname+'/rider.proto';

var driverPackageDefinition = protoLoader.loadSync(
    DRIVER_PROTO_PATH,
    {keepCase: true,
     longs: String,
     enums: String,
     defaults: true,
     oneofs: true
    });

var riderPackageDefinition = protoLoader.loadSync(
  RIDER_PROTO_PATH,
    {keepCase: true,
      longs: String,
      enums: String,
      defaults: true,
      oneofs: true
    });


var driver_proto =
    grpc.loadPackageDefinition(driverPackageDefinition).driver;
var rider_proto =
    grpc.loadPackageDefinition(riderPackageDefinition).rider; 

 
let isDriverOnline = false;
let currentDriverCall = null; 
let currentRiderCall = null;

function driverLocation(call) {
  console.log("Driver is online");
  isDriverOnline = true;

  call.on('data',function(request){
    console.log("Driver Location: "+request.latitude+" "+request.longitude);
  });
  call.on('end',function(){
    console.log("Driver is offline");
    isDriverOnline = false;
    call.end();
  });
}

function driverNewJob(call) {
  currentDriverCall = call;

  call.on('data', (request) => {
    currentRiderCall.write({
      action: request.action
    });
  });

  call.on('end',function(){
    currentDriverCall = null;
    call.end();
  });
}

function driverLocationRider() {
  console.log("driverLocationRider");
}

function requestRide(call) {
  console.log("Received a ride request");
  currentRiderCall = call;

  if (!isDriverOnline || !currentDriverCall) {
    console.log("No driver is online. Sending fallback response.");
    call.write({
      action: 7
    });
    return;
  }

  const rideRequest = call.request;

  // Simulate a ride being offered to the driver
  console.log(`Forwarding ride request to driver: lat=${rideRequest.pickupAddress}, long=${rideRequest.dropAddress}, time=${rideRequest.time}`);

  // Send ride request to driver stream
  currentDriverCall.write({
    action: 1,
    deliveryFrom: rideRequest.pickupAddress,
    deliveryTo: rideRequest.dropAddress,
    time: rideRequest.time,
    budget: 500 
  });

  call.write({
    action: 1
  });

  call.on('end',function(){
    currentRiderCall = null;
    call.end();
  });

}

function startGrpcServer() {
  const server = new grpc.Server();
  server.addService(driver_proto.DriverService.service, {
    driverLocation,
    driverNewJob,
  });
  server.addService(rider_proto.RiderService.service, {
    driverLocation: driverLocationRider,
    requestRide,
  });

  const PORT = process.env.PORT || 10000;
  const HOST = '0.0.0.0';

  server.bindAsync(
    `${HOST}:${PORT}`,
    grpc.ServerCredentials.createInsecure(),
    (error, port) => {
      if (error) {
        console.error('Failed to bind gRPC server:', error);
        process.exit(1);
      }
      console.log(`gRPC server running on ${HOST}:${port}`);
      server.start();
    }
  );
}

// Health check endpoint for Render
const httpServer = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('OK');
  } else {
    res.writeHead(404, { 'Content-Type': 'text/plain' });
    res.end('Not Found');
  }
});

function main() {
  startGrpcServer();
}

main();
