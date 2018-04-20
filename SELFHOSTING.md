# Selfhosting Guide
Well hey there! You've made it to this handy dandy guide about self hosting!

The original version of FlareBot was not meant to self host **at all** and so I (BinaryOverload) have spent a considerable amount of time to convert it to a self hostable version in light of the main project shutting down (RIP FlareBot :( )

To start with, you need to make sure you have these basic requirements:
 - Java 8 JRE and JDK
 - PostegreSQL v10 (Configured with a database for FlareBot's data. Usually called `flarebot`)
 - Git

 ### FlareBot Installation
 - Run `git clone https://github.com/binaryoverload/FlareBot-Selfhosting.git` to clone the repository into a local folder.
 - To generate the JAR file:
   - If you are on Windows, run `gradlew.bat shadowJar`.
   - If you are on Linux, run `./gradlew shadowJar`.
 - Find the JAR file called `FlareBot.jar` inside the `build/libs` folder and copy it to a directory of your choosing along with `config.example.yml`.
 - Edit the values inside `config.example.yml` as required and renamed the file to `config.yml`. Required values include:
   - Database username and password
   - Youtube API Token (Can be created [here](https://console.cloud.google.com/apis/))
   - Discord API Application Token (Can be created [here](https://discordapp.com/developers/applications/me))
   - A list of user IDs for the admins of the selfbot (Right click on a user in developer mode of Discord and click `Copy ID`)
 - Run the `FlareBot.jar` file using `java -jar FlareBot.jar` and voila! You should have a running bot!
 
 If this process does not work please contact me (BinaryOverload#2382) or join the [FlareBot Conversation Corner](https://discord.gg/8AVZ6RJ))!