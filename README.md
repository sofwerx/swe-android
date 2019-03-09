# swe-android

[![](https://jitpack.io/v/sofwerx/swe-android.svg)](https://jitpack.io/#sofwerx/swe-android)

A quick way to send sensor data to an OGC Sensor Observation Service - Transaction (SOS-T) server.

## Table of Contents
1. [Quick Start](#quick-start)
    1. Gradle
    1. Maven
1. [What is SOS-T](#what-is)
1. [How SOS-T Works](#brief-sos)
1. [How to Send Sensor Data](#how-to)

<h2 id="quick-start">Quick Start</h2>

### Gradle Setup

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.sofwerx:swe-android:1.0'
}
```

### Maven Setup

```xml
<!-- <repositories> section of pom.xml -->
<repository>
    <id>jitpack.io</id>
   <url>https://jitpack.io</url>
</repository>

<!-- <dependencies> section of pom.xml -->
<dependency>
    <groupId>com.github.sofwerx</groupId>
    <artifactId>swe-android</artifactId>
    <version>1.0</version>
</dependency>
```

<br/>

<h2 id="what-is">What is SOS-T?</h2>

The  [**Open Geospatial Consortium**](https://www.opengeospatial.org) is a group committed to making open standards for the geospatial community. One of those standards is the [**Sensor Observation Service**](https://www.opengeospatial.org/standards/sos) which defines how sensors can be managed and share their data. SOS-T is a specific capability that many SOS servers possess and is this library's focus. While powerful and flexible, SOS and SOS-T standards are sometimes difficult to begin using. This library is an effort to make all those standards and nuanced data formats happen in the background leaving you to focus on the app you're building and the great data it has to share with the world.

<h2 id="brief-sos">A Brief Overview of how SOS-T works</h2>

An **SOS-T server** and a **Sensor** have a specific pattern of interaction. This pattern is as follows:<br/>
1. The first time a **Sensor** wants to start providing data to a **SOS-T server**, the **Sensor** sends a message that describes itself. The **SOS-T server**, in response, sends back some identification names that the **SOS-T server** wants the **Sensor** to use to identify itself.<br/>
2. When the **Sensor** is ready to tell the **SOS-T server** about the data the **Sensor** will provide, the **Sensor** sends the **SOS-T server** a template that describes its data. The **SOS-T server** relies back with an indentification name that the **SOS-T server** would like the **Sensor** to use to label its data so the **SOS-T server** can match that data back to the **Sensor**.<br/>
3. Up until this point, the **Sensor** and **SOS-T server** have just negotiated descriptions and data formats, no sensor readings have actually gone from the **Sensor** to the **SOS-T server**. These two set-up steps only need to be done once.<br/>
4. Now, when the **Sensor** wants to send data, it just sends a short form with its data and the label that the **SOS-T server** provided earlier. If all goes well, the **SOS-T server** lets the **Sensor** know that the data was received in a short reply. The **Sensor** keeps repeating this step whenever it wants to send new data.

<h2 id="how-to">How to Send Sensor Data</h2>

To use this library, you should have some type of sensor in an app that needs to send data to an SOS-T server. This library started as a way to connect the SOFWERX [**TORGI**](https://github.com/sofwerx/TORGI) app to an SOS-T and is a good example of the library at work. This library is **not** the definitive answer on how to connect every sensor scenario with an SOS-T server, but it helped the TORGI effort and it's offered in hopes it will help other sensor developers as well. You can see the specific implementation of this library in the [**TorgiService**](https://github.com/sofwerx/TORGI/blob/master/torgi/src/main/java/org/sofwerx/torgi/service/TorgiService.java). <br/><br/>

### Step 1: build your sensor

You can build a sensor by providing a human-readable name that uniquely identifies this one particular sensor, a unique ID for this sensor, a title for this type of sensor, and a human-readable description of the sensor.
```java
SosSensor sosSensor = new SosSensor("Sensor 18742","sensor18742","TORGI","Tactical Observation of RF and GNSS Interference sensor");
```
<br/>
Next you want to add SensorMeasurements to your sensor, SensorMeasurements describe what the sensor measures. For our example, our sensor measures the amount of "foo" at a particular time and location like this:
```java
sosMeasurementFoo = new SensorMeasurement(new SensorResultTemplateField("foo","www.your_link_that_describes_the_foo_standard.com","foo per inches")); //the url provided in the second field helps others understand about this idea of "foo" if you dont have a link, you can provide some placeholder or a way to contact you
sosMeasurementTime = new SensorMeasurementTime();
sosMeasurementLocation = new SensorMeasurementLocation();
sosSensor.addMeasurement(sosMeasurementFoo);
sosSensor.addMeasurement(sosMeasurementTime);
sosSensor.addMeasurement(sosMeasurementLocation);
```
<br/>

### Step 2: describe the SOS-T server

Now you need to tell the library about where the sensor should send its data
```java
SosService sosService = new SosService(context, sosSensor,"www.your-sos-t-server.com", true, true);
```
<br/>
Those last two boolean values deserve a bit more attention. The first, *turnOn* means that if set to true, the library will immediately try to contact the **SOS-T server** and handle all the initial information about what the **Sensor** intends to provide.<br/>
The second boolean, *enableIpcBroadcast* means that the library will send out your sensor data to other apps on your same device. This is included primarily to support a future link with [**OpenSensorHub-Android**](https://github.com/opensensorhub/osh-android). If you want to send your sensor data to other apps on the same device, but not out over the internet to an SOS-T server, just provide *null* for the sosServerURL.
<br/>

### Step 3: send your data

When you have something to report to the **SOS-T server**, you can update the data in your SensorMeasurements
```java
sosMeasurementTime.setValue(System.currentTimeMillis());
sosMeasurementLocation.setLocation(latitude,longitude,altitude);
sosMeasurementRisk.setValue(foo);
```
and then send that data off to the **SOS-T server**
```java
sosService.broadcastSensorReadings();
```
<br/>
You can repeat this step as often as needed. There is no requirement to handle the connection with the server. The library runs on another thread and will make the connection whenever is needed and pass and receive information automatically.
<br/>

### Step 4: disconnecting

When you are finally done communicating with the server, clean up the server connection by calling:
```java
sosService.shutdown();
```
