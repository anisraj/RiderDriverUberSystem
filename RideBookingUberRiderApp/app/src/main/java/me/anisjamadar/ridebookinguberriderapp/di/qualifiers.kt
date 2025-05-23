package me.anisjamadar.ridebookinguberriderapp.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LoggingInterceptor