# Control an LED using Android App via ARTIK Cloud

Build a remote light control system using ARTIK Cloud, LED, Raspberry Pi, and an Android application. The system contains the following components:

 - An IoT device that acts on received commands, turn on/off the LED, and finally sends back the latest state.
 - An Android application that sends commands to the device and displays the latest state of the device.

Introduction
-------------

The tutorial [Your first IoT remote control system](http://developer.artik.cloud/documentation/tutorials/an-iot-remote-control.html) at http://developer.artik.cloud/ describes what the system does and how it is implemented.

This repository contains the following software:

 - A Node.js script running on the Raspberry Pi
 - An Android application running on the Android phone

Android Application
-------------

The root directory of the application is `android-simple-controller`.

Consult [set up the Android project](http://developer.artik.cloud/documentation/tutorials/an-iot-remote-control.html#set-up-the-android-project) in the tutorial to learn the prerequisites and installation steps.

Nodejs Program for Raspberry Pi
-------------

The code is located in `raspberrypi` directory. Consult [Set up the Raspberry Pi](http://developer.artik.cloud/documentation/tutorials/an-iot-remote-control.html#set-up-the-software) in the tutorial to install the packages and to run the program on the Pi.

More about ARTIK Cloud
---------------------

If you are not familiar with ARTIK Cloud, we have extensive documentation at https://developer.artik.cloud/documentation

The full ARTIK Cloud API specification can be found at https://developer.artik.cloud/documentation/api-spec

Check out advanced sample applications at https://developer.artik.cloud/documentation/samples/

To create and manage your services and devices on ARTIK Cloud, create an account at https://developer.artik.cloud

Also see the ARTIK Cloud blog for tutorials, updates, and more: http://artik.io/blog/cloud

License and Copyright
---------------------

Licensed under the Apache License. See [LICENSE](https://github.com/artikcloud/tutorial-iot-control-light/blob/master/LICENSE).

Copyright (c) 2016 Samsung Electronics Co., Ltd.
