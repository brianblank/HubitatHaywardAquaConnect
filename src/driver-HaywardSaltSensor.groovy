metadata {
   definition (name: "Hayward Salt Sensor", namespace: "brianblank", author: "Brian Blank") {
        attribute "saltPPM", "int"
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

private sendSaltEvent(name, val) {
	sendEvent(name: "${name}", value: val, unit: "ppm", descriptionText: getDescriptionText("${name} is ${val}ppm"), isStateChange: true)
}

def setSaltPpm(value) {
	log.debug "'${device.displayName}'.setSaltPpm(${value}) was called"
	sendSaltEvent("saltPPM", value)
}
