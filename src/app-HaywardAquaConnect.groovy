definition(
    name: "Hayward AquaConnect",
    namespace: "brianblank",
    author: "Brian Blank",
    description: "Hayward AquaConnect Controller for Hubitat",
    category: "Pool Automation",
    iconUrl: "",
    iconX2Url: "",
    documentationLink: "https://github.com/brianblank"
)

private def get_APP_VERSION() {
	return "0.0.1"
}

preferences {
    page(name: "deviceConfigPage", title: "Device Configuration Page", install: true, uninstall: true) {
         section("About") {	
		paragraph "Version ${get_APP_VERSION()}"
		paragraph "If you like this smartapp, please support the developer via Venmo and click on the Venmo link below " 
			href url:"https://venmo.com/u/brianblank01",
					title:"Venmo donation..."
		paragraph "Copyright©2023 Brian Blank"
			href url:"https://github.com/brianblank/HubitatHaywardAquaConnect", style:"embedded", required:false, title:"More information...", 
				description: "https://github.com/brianblank/HubitatHaywardAquaConnect"
	}
        section("Settings") {
            input name: "networkAddress", type: "text", title: "Network Address (IP or DNS) of your AquaConnect Hub: "
            input name: "pollingFrequencySeconds", type: "number", title: "Polling Frequency (in seconds):", defaultValue: 4
            input name: "buttonPressDelayMilliseconds", type: "number", title: "Button Press Delay (in MilliSeconds):", defaultValue: 500
            input name: "startingHeatingSetpoint", type: "number", title: "Initial Heat Set Point (65 - 104) in degrees:", defaultValue: 85
            input name: "logEnabled", type: "bool", title: "Enable debug logging for 5 minutes", defaultValue: false
        }
    }
}

def htmlEncode(String s) {
    return groovy.xml.XmlUtil.escapeXml(s)
}

def logsOff(){
    log.warn "debug logging disabled..."
    device.updateSetting("logEnabled",[value:"false",type:"bool"])
}

// Called when app first installed
def installed() {
    // for now, just write entry to "Logs" when it happens:
    log.trace "installed()"
    
    initialize()
    
    if (logEnabled) runIn(5 * 60, "logsOff")
    schedule("0/${pollingFrequencySeconds} * * ? * * *", "sendRequest")
}

// Called when user presses "Done" button in app
def updated() {
    log.trace "updated()"
    
    if (logEnabled) runIn(5 * 60, "logsOff")

    unschedule("sendRequest")
    schedule("0/${pollingFrequencySeconds} * * ? * * *", "sendRequest")
}

// Called when app uninstalled
def uninstalled() {
   log.trace "uninstalled()"
   // Most apps would not need to do anything here
}

def initialize() {
    def poolTemp = getChildDevice("HaywardPoolTemp_${app.id}")
    if(!poolTemp) poolTemp = addChildDevice("brianblank", "Hayward Temperature Sensor", "HaywardPoolTemp_${app.id}", null, [label: "Hayward Pool Temp", name: "Hayward Pool Temp"])
    poolTemp.setTemperature(0)

    def airTemp = getChildDevice("HaywardAirTemp_${app.id}")
    if(!airTemp) {
        airTemp = addChildDevice("brianblank", "Hayward Temperature Sensor", "HaywardAirTemp_${app.id}", null, [label: "Hayward Air Temp", name: "Hayward Air Temp"])
        airTemp.setTemperature(0)
    }

    def saltSensor = getChildDevice("HaywardSaltSensor_${app.id}")
    if(!saltSensor) {
        saltSensor = addChildDevice("brianblank", "Hayward Salt Sensor", "HaywardSaltSensor_${app.id}", null, [label: "Hayward Salt Sensor", name: "Hayward Salt Sensor"])
        saltSensor.setSaltPpm(0)
    }
    
    def filterSwitch = getChildDevice("HaywardFilterSwitch_${app.id}")
    if(!filterSwitch) {
        filterSwitch = addChildDevice("brianblank", "Hayward Switch", "HaywardFilterSwitch_${app.id}", null, [label: "Hayward Filter Switch", name: "Hayward Filter Switch"])
        filterSwitch.setButtonID("08")
    }
    
    def lightsSwitch = getChildDevice("HaywardLightsSwitch_${app.id}")
    if(!lightsSwitch) {
        lightsSwitch = addChildDevice("brianblank", "Hayward Switch", "HaywardLightsSwitch_${app.id}", null, [label: "Hayward Lights Switch", name: "Hayward Lights Switch"])
        lightsSwitch.setButtonID("09")
    }
    
    def heaterSwitch = getChildDevice("HaywardHeaterSwitch_${app.id}")
    if(!heaterSwitch) {
        heaterSwitch = addChildDevice("brianblank", "Hayward Heater Switch", "HaywardHeaterSwitch_${app.id}", null, [label: "Hayward Heater Switch", name: "Hayward Heater Switch"])
    }
}

def sendRequest(data) {
    def postParams = [
		uri: "http://${networkAddress}/WNewSt.htm",
		contentType: "text/plain;charset=UTF-8",
		body : "Update Local Server&"
	]
    
	asynchttpPost('processResponse', postParams, [dataitem1: "datavalue1"])
}

def String getDeviceStatus(char[] cHaywardDataRawLeds, int charOffset, int hexOffset) {
    // Defined in WebsFuncs.js, function DecodeRawLedData
    // "3" == "WEBS_NOKEY"
    // "4" == "WEBS_OFF"
    // "5" == "WEBS_ON"; 
    // "6" == "WEBS_BLINK" ; 

    switch (String.format("%02X", (int) cHaywardDataRawLeds[charOffset]).substring(hexOffset, hexOffset+1)) {
        case "4":
            return "off"
        case "5":
            return "on"
        case "6":
            return "blink"
        case "3": 
        default:
            return "nokey"
    }
}

def processResponse(response, data) {
    if(response.getStatus() == 200 || response.getStatus() == 207) {
        def String resp = response.getData().toString() 
        def String[] responseLines = resp.substring(resp.indexOf("<body>") + 6, resp.indexOf("</body")).split('xxx')
        for(int i = 0;i<3;i++) { responseLines[i] = responseLines[i].trim() }
        def String haywardDataRawLeds = responseLines[2]
        def char[] cHaywardDataRawLeds = haywardDataRawLeds.toCharArray();
        def String filterState = getDeviceStatus(cHaywardDataRawLeds, 1, 1)
        def String lightsState = getDeviceStatus(cHaywardDataRawLeds, 2, 0)
        def String heaterState = getDeviceStatus(cHaywardDataRawLeds, 3, 0)
        
        def filterSwitch = getChildDevice("HaywardFilterSwitch_${app.id}")
        if(! filterSwitch.currentValue("switch").equals(filterState)) {
            filterSwitch.setState(filterState)
        }

        def lightsSwitch = getChildDevice("HaywardLightsSwitch_${app.id}")
        if(! lightsSwitch.currentValue("switch").equals(lightsState)) {
            lightsSwitch.setState(lightsState)
        }
        
        def heaterSwitch = getChildDevice("HaywardHeaterSwitch_${app.id}")
        if(! heaterSwitch.currentValue("switch").equals(heaterState)) {
            heaterSwitch.setState(heaterState)
        }

        if(logEnabled) log.debug "haywardDataRawLeds='${haywardDataRawLeds}'; filterState='${filterState}'; lightsState='${lightsState}'; heaterState='${heaterState}'"
        if(logEnabled) log.debug "Response Line 2: '${responseLines[1]}'"
        if(logEnabled) log.debug "Response Line 1: '${responseLines[0]}'"
        
        switch (responseLines[0]) {
            case "Salt Level":
                  def long haywardDataSaltLevel = responseLines[1].split(' ')[0].toInteger()
                  def saltSensor = getChildDevice("HaywardSaltSensor_${app.id}")
                  if(haywardDataSaltLevel != saltSensor.saltPPM) {
                      saltSensor.setSaltPpm(haywardDataSaltLevel)
                  }
                  if(logEnabled) log.debug "haywardDataSaltLevel = ${haywardDataSaltLevel}"
            break
            case "Pool Chlorinator":
                  def short haywardDataPoolChlorinator = responseLines[1].substring(0, responseLines[1].length()-1).toInteger()
                  if(logEnabled) log.debug "haywardDataPoolChlorinator = ${haywardDataPoolChlorinator}"
            break
            case ~/^Air Temp.*/:
                  def short haywardDataAirTemp = responseLines[0].substring(11, responseLines[0].length() - 6).toInteger()
                  def airTemp = getChildDevice("HaywardAirTemp_${app.id}")
                  if(haywardDataAirTemp != airTemp.currentValue("temperature") ) { // airTemp.currentTemperature) {
                      airTemp.setTemperature(haywardDataAirTemp)
                  }
                  if(logEnabled) log.debug "haywardDataAirTemp = ${haywardDataAirTemp}"
            break
            case ~/^Pool Temp.*/:
                  def short haywardDataPoolTemp = responseLines[0].substring(11, responseLines[0].length() - 6).toInteger()
                  def poolTemp = getChildDevice("HaywardPoolTemp_${app.id}")
                  if(haywardDataPoolTemp != poolTemp.currentValue("temperature") ) { // poolTemp.currentTemperature) {
                      poolTemp.setTemperature(haywardDataPoolTemp)
                  }
                  if(logEnabled) log.debug "haywardDataPoolTemp = ${haywardDataPoolTemp}"
            break
            default:
                  if(logEnabled) log.debug "Default Response Line 2: '${responseLines[1]}'"
                  if(logEnabled) log.debug "Default Response Line 1: '${responseLines[0]}'"
            break
        }
    } else {
        log.error "Hayward Error Response Code: ${response.status}"
    }
}

def String getDisplay(String payload = "Update Local Server&") {
    def String retVal = ""

    def postParams = [
		uri: "http://${networkAddress}/WNewSt.htm",
        contentType: "text/plain;charset=UTF-8",
        requestContentType: "application/x-www-form-urlencoded",
        body: "${payload}"
	]

    if(logEnabled) log.debug "getDisplay(payload='${payload}')"
    if(logEnabled) log.debug "postParams='${postParams}')"
    
    try {
        httpPost(postParams) { response ->
            if (response.status == 200) {
                if(logEnabled) log.debug "getDisplay(payload='${payload}') -> Response Code ${response.status}"
                
                def String resp = response.getData().getText() 
                if(logEnabled) log.debug "${htmlEncode(resp)}"
                if(resp.substring(resp.indexOf("<body>")) < resp.substring(resp.indexOf("</body>"))) {
                    throw new Exception("<body> before </body> : '${htmlEncode(resp)}")
                }
                def String[] responseLines = resp.substring(resp.indexOf("<body>") + 6, resp.indexOf("</body")).split('xxx')
                for(int i = 0;i<3;i++) { responseLines[i] = responseLines[i].trim() }
                retVal = "${responseLines[0]};${responseLines[1]}"
            } else {
                log.error "getDisplay(payload='${payload}') -> Error Code ${response.status}"
            }
        }
    } catch (groovyx.net.http.HttpResponseException e) {
        log.error "getDisplay(payload='${payload}') exception ${e}"
	}
    return retVal
}

def String pressButton(button) {
    def String retVal = ""
    // Button:
    //    01 - Right
    //    02 - Menu
    //    03 - Left
    //    05 - Minus
    //    06 - Plus
    //    08 - Filter
    //    09 - Lights
    return getDisplay("KeyId=${button}");
}
