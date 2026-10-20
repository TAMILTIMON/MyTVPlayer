MY TV PLAYER - build it with GitHub (free)

1. Make a free account at github.com and sign in.
2. Click the + at the top right > New repository. Name it MyTVPlayer.
   Choose Public (free builds are unlimited for public repos). Create it.
3. On the new repository page click "uploading an existing file".
4. Unzip MyTVPlayer.zip on your PC. Open the unzipped folder, select
   EVERYTHING inside it (including the .github folder) and drag it into
   the GitHub upload page. Wait until all files finish uploading.
   If the .github folder does not appear in the list, see the note below.
5. Press "Commit changes". The build starts by itself.
6. Click the "Actions" tab. Wait about 5 to 10 minutes for a green tick.
   A red cross means the build failed: open it and send Claude the error.
7. Open the finished run, scroll to "Artifacts", click MyTVPlayer-apk.
   A zip downloads. Unzip it to get app-debug.apk.
8. Copy app-debug.apk into your platform-tools folder and run:
      adb connect 192.168.1.33:5555
      adb install -r app-debug.apk
9. Open "My TV Player" on the TV. It shows an address like
   http://192.168.1.33:8080 - open it on your phone (same Wi-Fi) to add links.

NOTE if .github is missing after upload: click Add file > Create new file,
type  .github/workflows/build.yml  as the file name (the slashes make the
folders), paste in the contents of that file from this zip, and commit.

Add it to the tv-detox home screen:
   adb shell am start -n com.ansub.tvhome/.MainActivity --es apps org.videolan.vlc,in.startv.hotstar,com.netflix.ninja,com.mxtech.videoplayer.ad,com.mxtech.videoplayer.television,com.mytv.player

Remote keys while playing: OK = pause/play, Left/Right = back/forward 10 s,
Up = next video, Down = previous video, Back = leave.
The web page on the phone works only while the app is open on the TV.
To remove the app:  adb uninstall com.mytv.player
