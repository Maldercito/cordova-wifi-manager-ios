
# Hotspot for ios

Thank to this plugin you can connect and disconnect from your hotspot in ios automatically

### Supported platforms

- iOS

### Installation

To install the plugin

```bash
$ ionic cordova plugin add https://elibo@bitbucket.org/inubo/cordova-wifi-ios.git
```

### Before using
Before using the plugin you need to declare a variable in the view where you intent to call the plugin

```bash
declare var window: any;
```

### Connect
Then to use it to connect to a hotspot you just have to call to the next function. You need to pass the ssid of the hotspot, the password and a boolean to indicate if the security is base in wep or in another type of security

```bash
 window.hotspot.connect('ssid', 'password', 'iswep',
 (succes)=>(),
 (err)=>())
```

### Disconnect
To disconnect you just need to pass the ssid and that's it
```bash
window.hotspot.disconnect('ssid',(success)=>(),(err)=>())
```
