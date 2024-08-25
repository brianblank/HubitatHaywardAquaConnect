metadata {
   definition (name: "Hayward Heater Switch", namespace: "brianblank", author: "Brian Blank") {
       capability "Switch"
       capability "Thermostat Heating Setpoint"
       capability "Temperature Measurement"
       
       // command "setTemperature", ["NUMBER"]

       // attribute switch - ENUM ["on", "off"]
       // attribute heatingSetpoint - NUMBER, unit:°F || °C
       attribute "currentHeatingValue", "String"
   }

   preferences {
      // none in this driver
   }
}

private getDescriptionText(msg) {
	def descriptionText = "${device.displayName} ${msg}"
	return descriptionText
}

private sendEvent(name, val) {
	sendEvent(name: "${name}", value: val, descriptionText: getDescriptionText("${name} is ${val}"), isStateChange: true)
}

private sendTemperatureEvent(name, val) {
	sendEvent(name: "${name}", value: val, unit: "°${getTemperatureScale()}", descriptionText: getDescriptionText("${name} is ${val} °${getTemperatureScale()}"), isStateChange: true)
}

def setState(state) {
    log.debug "'${device.displayName}'.setState('${state}') was called"
    sendEvent("physicalSwitch", "${state}")
}

def void installed() {
    log.debug getDescriptionText("installed()")
    sendEvent("heatingSetpoint", parent.startingHeatingSetpoint)
    changeTemperatureSettingStep1("init")
}

def void updated() {
   log.debug getDescriptionText("updated()")
}

def void setHeatingSetpoint(temperature) {
    def prevHeatingSetpoint = device.currentValue("heatingSetpoint")
    temperature = Math.max(Math.min("${temperature}".toInteger(), 104), 65)
    if(parent.logEnabled) log.debug parent.htmlEncode("setHeatingSetpoint(${temperature}); prevHeatingSetpoint=${prevHeatingSetpoint}; currentHeatingValue=${device.currentValue("currentHeatingValue")}")
    sendEvent("heatingSetpoint", temperature)
    if(parent.logEnabled) log.debug parent.htmlEncode("prevHeatingSetpoint=${prevHeatingSetpoint}; currentHeatingValue=${device.currentValue("currentHeatingValue")}")
    if(parent.logEnabled) log.debug device.currentValue("currentHeatingValue").equals("${prevHeatingSetpoint}")
    if(device.currentValue("currentHeatingValue").trim().equals("${prevHeatingSetpoint}".trim())) {
        changeTemperatureSettingStep1("${temperature}")
    }
}

def String getCurrentHeatingValue(String line2) {
    if(parent.logEnabled) log.debug "getCurrentHeatingValue('${parent.htmlEncode(line2)}')"
    // Get rid of "<span class="WBON">" and "</span>"
    if(line2.indexOf("<span") >= 0) {
        line2 = line2.substring(line2.indexOf(">")+1, line2.indexOf("</span>"))
    }
    if(parent.logEnabled) log.debug "Updated line2=('${parent.htmlEncode(line2)}')"

    if(line2.toLowerCase().equals("off"))
        return "off"
    else
        return line2.substring(0, line2.length()-6)
}

def void off() {
    if(parent.logEnabled) log.debug "${device.displayName} off()"
    if(parent.logEnabled) log.debug "device.currentValue('switch') == '${device.currentValue("switch")}'"
    if(! device.currentValue("currentHeatingValue").equals("off")) {
        sendEvent("switch", "off")
        changeTemperatureSettingStep1("off")
    }
}

def void on() {
    if(parent.logEnabled) log.debug "${device.displayName} on()"
    if(parent.logEnabled) log.debug "device.currentValue('switch') == '${device.currentValue("switch")}'"
    if(! device.currentValue("currentHeatingValue").equals("${device.currentValue("heatingSetpoint")}")) {
        sendEvent("switch", "on")
        changeTemperatureSettingStep1("${device.currentValue("heatingSetpoint")}")
    }
}

def changeTemperatureSettingStep1(String newTemp) {
    if(parent.logEnabled) log.debug getDescriptionText("changeTemperatureSettingStep1(${newTemp})")
    
    if(! parent.getDisplay().equals("Settings;Menu")) {
        parent.pressButton("02") // 02 = Menu Button
        runInMillis(parent.buttonPressDelayMilliseconds, "changeTemperatureSettingStep1", [data: "${newTemp}"])
    } else {
        parent.pressButton("01") // 01 = Right Button
        runInMillis(parent.buttonPressDelayMilliseconds, "changeTemperatureSettingStep2", [data: "${newTemp}"])
    }
}

def changeTemperatureSettingStep2(String newTemp) {
    if(parent.logEnabled) log.debug getDescriptionText("changeTemperatureSettingStep2(${newTemp})")
    
    if(! parent.getDisplay().split(";")[0].equals("Pool Heater1")) {
        parent.pressButton("01") // 01 = Right Button
        runInMillis(parent.buttonPressDelayMilliseconds, "changeTemperatureSettingStep2", [data: "${newTemp}"])
    } else {
        runInMillis(parent.buttonPressDelayMilliseconds, "changeTemperatureSettingStep3", [data: "${newTemp}"])
    }
}

def changeTemperatureSettingStep3(String newTemp) {
    if(parent.logEnabled) log.debug getDescriptionText("changeTemperatureSettingStep3(${newTemp})")
    
    display = parent.getDisplay()
    if(parent.logEnabled) log.debug "diplay='${parent.htmlEncode(display)}'"

    def currTemp = getCurrentHeatingValue(display.split(";")[1])
    if(parent.logEnabled) log.debug "Final currTemp='${parent.htmlEncode(currTemp)}'; newTemp='${newTemp}'"
    
    sendEvent("currentHeatingValue", "${currTemp}")
    
    if(!newTemp.equals("init") && !currTemp.equals(newTemp)) {
        // When turning on, start from 104 and go down towards the newTemp so that the heat turns on faster
        // When turning off, go down towards off to turn off the heater faster
        // However, if we overshoot the target number, we should attempt to go the opposite direction so we don't have to loop around
        def int iCurrTemp = (currTemp.equals("off") ? 105 : currTemp.toInteger())
        def int iNewTemp = (newTemp.equals("off") ? 64 : newTemp.toInteger())
        if(iCurrTemp > iNewTemp) {
            parent.pressButton("05") // 05 = Minus Button
        } else {
            parent.pressButton("06") // 06 = Plus Button
        }
        runInMillis(parent.buttonPressDelayMilliseconds, "changeTemperatureSettingStep3", [data: "${newTemp}"])
    } else {
        if(parent.logEnabled) log.debug getDescriptionText("changeTemperatureSettingStep3(${newTemp}); currTemp='${currTemp}'")
    }
}

def setTemperature(temperature) {
	"'${device.displayName}'.setTemperature(${temperature}) was called"
	sendTemperatureEvent("temperature", temperature)
}
