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
let currentRiderLocationStream = null;



function driverLocation(call) {
  console.log("Driver is online");
  isDriverOnline = true;

  call.on('data',function(request){
    console.log("Driver Location: "+request.latitude+" "+request.longitude);

    if (currentRiderLocationStream != null) {
      currentRiderLocationStream.write({
        latitude: request.latitude,
        longitude: request.longitude
      });
    }

  });
  call.on('end',function(){
    console.log("Driver is offline");
    isDriverOnline = false;
    call.end();
  });

  call.on('error', function(err) {
    console.error("Driver location stream error:", err);
  });
}

function driverLocationRider(call) {
  currentRiderLocationStream = call;
  call.on('end',function(){
    currentRiderLocationStream = null;
    call.end();
  });
  call.on('error', function(err) {
    console.error("Rider location stream error:", err);
  });
}

function driverNewJob(call) {
  currentDriverCall = call;

  call.on('data', (request) => {
    console.log("driverNewJob action received:", request);
    currentRiderCall.write({
      action: request.action
    });
  });

  call.on('end',function(){
    currentDriverCall = null;
    call.end();
  });

  call.on('error', function(err) {
    console.error("Driver new job stream error:", err);
  });
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

function main() {
  var server = new grpc.Server();
  server.addService(driver_proto.DriverService.service, {
    driverLocation: driverLocation,
    driverNewJob: driverNewJob
  });
  server.addService(rider_proto.RiderService.service, {
    driverLocation: driverLocationRider,
    requestRide: requestRide
  });

  server.bindAsync('0.0.0.0:50051', grpc.ServerCredentials.createInsecure(), () => {
    server.start();
  });
}

main();
