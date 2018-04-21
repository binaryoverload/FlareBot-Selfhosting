# Selfhosting Guide
Well hey there! You've made it to this handy dandy guide about self hosting!

The original version of FlareBot was not meant to self host **at all** and so I (BinaryOverload) have spent a considerable amount of time to convert it to a self hostable version in light of the main project shutting down (RIP FlareBot :( )

To start with, you need to make sure you have these basic requirements:
 - Java 8 JRE and JDK
 - PostegreSQL v10 (Configured with a database for FlareBot's data. Usually called `flarebot`)
 - Git
 
##### Disclaimer
This guide assumes basic command-line experience and knowing how to run Java programs. If this is not you, then you shouldn't be doing this!

 ### FlareBot Installation
 1. Run `git clone https://github.com/binaryoverload/FlareBot-Selfhosting.git` to clone the repository into a local folder.
 2. To generate the JAR file:
    - If you are on Windows, run `gradlew.bat shadowJar`.
    - If you are on Linux, run `./gradlew shadowJar`.
 3. Find the JAR file called `FlareBot.jar` inside the `build/libs` folder and copy it to a directory of your choosing along with `config.example.yml`.
 4. Edit the values inside `config.example.yml` as required and renamed the file to `config.yml`. Required values include:
    - Database username and password
    - Youtube API Token (Can be created [here](https://console.cloud.google.com/apis/))
    - Discord API Application Token (Can be created [here](https://discordapp.com/developers/applications/me))
    - A list of user IDs for the admins of the selfbot (Right click on a user in developer mode of Discord and click `Copy ID`)
 5. Run the `FlareBot.jar` file using `java -jar FlareBot.jar` and voila! You should have a running bot!
 
 If this process does not work please contact me (BinaryOverload#2382) or join the [FlareBot Conversation Corner](https://discord.gg/8AVZ6RJ))!
 
 ### Setup FlareBot as a service (Recommended)
 
 
 Setting up FlareBot as a service is best practice for running it on a server and is very simple to do!
 You need to make sure FlareBot is in the directory you want it to run in as **you will not be able to move it easily after this!** 
 Please also make sure you are logged into a user that has all permissions for the directory that you will run FlareBot from (This is usually **not** root!).
If you need assistance setting this up, either look it up or ask me for help!
 
 **This process will only work on Linux based systems!**
 
 1. Move the `jarservice` executable from the repository to the directory you want to run FlareBot from.
 2. Run `sudo ./jarservice FlareBot.jar` to create the service.
 3. Voila!
 
  - To tell the bot to start on runtime, run `sudo systemctl enable FlareBot`. 
  -  To start the bot, run `sudo systemctl start FlareBot`.
  - To view logs from the bot, you can run `sudo journalctl -o cat -u FlareBot -f` or run `tail -f latest.log` in the run directory.
  
Once this is completed, you are able to edit the config and update the bot as much as you want **but** everything has to be named the same thing as you started with.