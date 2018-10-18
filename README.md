# Data collection applicaton for paper "Discovering User-context in Indoor Space".

We discover user-context in indoor space by combining activity and location context for user. 
This application is used for data collection purpose. 

The following sensor's data collected from android phone:

- Gyroscope
- Accelerometer

The coordinates of each user were collected by an indoor positioning application - [BuildNGo](https://sailstech.com/).

Application can be reused in the following way:

1. Define ground truth data format in ```strings.xml``` file:
  - ```actions_array```: Actions, which will be performed by participants (write, type, listen, etc.)
  - ```room_array```: Locations, where participants located
  - ```user_array```: Participants, define unique id for each one
2. Provide BuildNGo token and building ID in ```MainActivity.java```:
  
  ```java
  private static String TOKEN = "...";
  private static String BUILDING_ID = "...";
  ```
Next step after collecting data is preprocessing. Follow [this](https://github.com/bolatuly/HHAR-Data-Process) project description. 
