# Anagram
P2P Video Chat for Android Devices
<br>

## Build
To run the video chat, specify your STUN and TURN servers in app/src/main/java/app/anagram/Config.java.
<br>
Then set ROLE = ClientRole.master and build the first APK for the caller.
<br>
After that, change ROLE to ClientRole.slave and build the second APK for the receiver.
<br>
<br>

## Run
To start the video chat, share one of the built APK with the other party. If you plan to communicate with multiple users, give them the slave build and keep the master build for yourself.
<br>
The launch order of the app (master or slave build) does not matter.
<br>
To end the video chat and close the app, press the Back button in the bottom navigation bar.
<br>
<br>

## License

Copyright 2013-2025 Roman Popov

Licensed under the GNU GPLv3: https://www.gnu.org/licenses/gpl-3.0.html
