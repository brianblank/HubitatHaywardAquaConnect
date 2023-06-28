metadata {
   definition (name: "Hayward Temperature Sensor", namespace: "brianblank", author: "Brian Blank") {
       capability "Temperature Measurement"
       
       // command "setTemperature", ["NUMBER"]
   }

   preferences {
      // none in this driver
   }
    
}

def installed() {
   log.debug "installed()"
}

def updated() {
   log.debug "updated()"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	return descriptionText
}

private sendTemperatureEvent(name, val) {
	sendEvent(name: "${name}", value: val, unit: "°${getTemperatureScale()}", descriptionText: getDescriptionText("${name} is ${val} °${getTemperatureScale()}"), isStateChange: true)
}

def setTemperature(temperature) {
	"'${device.displayName}'.setTemperature(${temperature}) was called"
	sendTemperatureEvent("temperature", temperature)
}
