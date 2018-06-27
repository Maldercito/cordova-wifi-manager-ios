import NetworkExtension
import SystemConfiguration.CaptiveNetwork

@objc(hotspot) class hotspot: CDVPlugin{
  
   @objc(connect:)
    func connect(command: CDVInvokedUrlCommand){
        let name = command.arguments[0] as? String ?? ""
        let pass = command.arguments[1] as? String ?? ""
        let wep = command.arguments[2] as? Bool ?? false

        let hotspotConfig = NEHotspotConfiguration(ssid: name, passphrase: pass, isWEP: wep)

        NEHotspotConfigurationManager.shared.apply(hotspotConfig) {[unowned self] (error) in
			if let error = error {
				self.sendError(msg: error.localizedDescription, command: command)
			}
			else {
				self.sendSuccess(msg: "OK", command: command)
			}
        }
	}

 @objc(getSSID:)
	func getSSID(command: CDVInvokedUrlCommand) {
		let name = command.arguments[0] as? String ?? ""

		NEHotspotConfigurationManager.shared.getConfiguredSSIDs() { (ssids) in
			if ssids.count > 0 {
				self.getSSIDS(name: name, command: command);
			} else {
				self.sendError(msg: "No saved networks",command: command)
			}
		}
}


	@objc(disconnect:)
	func disconnect(command: CDVInvokedUrlCommand){
		let name = command.arguments[0] as? String ?? ""

		NEHotspotConfigurationManager.shared.removeConfiguration(forSSID: name)  
		self.sendSuccess(msg: "DISCONNECTED", command: command)
	}



	private func getSSIDS(name: String, command: CDVInvokedUrlCommand){
		if let interfaces = CNCopySupportedInterfaces() as NSArray? { 
			for interface in interfaces {
				if let interfaceInfo = CNCopyCurrentNetworkInfo(interface as! CFString) as NSDictionary? {
              	  let ssid = interfaceInfo[kCNNetworkInfoKeySSID as String] as? String
					if (ssid == name) {
						self.sendSuccess(msg: name, command: command)
					} else {
						self.sendError(msg: "Network connected unknown",command: command)
					}
			}
		}
	}
}

	private func sendError(msg: String, command: CDVInvokedUrlCommand){
		let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAs: msg)
		self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
	}

	private func sendSuccess(msg: String, command: CDVInvokedUrlCommand){
		let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: msg)
		self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
	}

}