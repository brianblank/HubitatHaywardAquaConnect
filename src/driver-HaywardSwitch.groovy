metadata {
   definition (name: "Hayward Switch", namespace: "brianblank", author: "Brian Blank") {
       capability "Switch"
       
       // attribute switch - ENUM ["on", "off"]

       attribute "buttonId", "String"
   }

   preferences {
      // none in this driver
   }
    
}

def void installed() {
   log.debug "'${device.displayName}' installed()"
}

def void updated() {
   log.debug "'${device.displayName}' updated()"
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	return descriptionText
}

private sendEvent(name, val) {
	sendEvent(name: "${name}", value: val, descriptionText: getDescriptionText("${name} is ${val}"), isStateChange: true)
}

def setButtonID(String buttonId) {
    sendEvent(name: "buttonId", value: "${buttonId}")
}

def void off() {
    if(parent.logEnabled) log.debug "${device.displayName} off()"
    if(parent.logEnabled) log.debug "device.currentValue('switch') == '${device.currentValue("switch")}'"
    if(device.currentValue("switch").equals("on")) {
        parent.pressButton(device.currentValue('buttonId'))
    }
}

def void on() {
    if(parent.logEnabled) log.debug "${device.displayName} on()"
    if(parent.logEnabled) log.debug "device.currentValue('switch') == '${device.currentValue("switch")}'"
    if(device.currentValue("switch").equals("off")) {
        parent.pressButton(device.currentValue('buttonId'))
    }
}

def setState(state) {
    log.debug "'${device.displayName}'.setState('${state}') was called"
    sendEvent("switch", "${state}")
}
