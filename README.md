# HubitatHaywardAquaConnect
Hubitat Integration for Hayward AquaConnect

The goal of this project is to connect my pool to my home automation network via Hubitat.  A secondary goal was to eliminate the Hayward AquaConnect from talking to the cloud service over the internet due to security concerns (the device talks to the cloud via an insecure port 80).

My Pool setup consists of:
* Hayward Aqua Logic Control Board
* Jandy JXL 400K BTU Pool Heater
* Hayward AquaConnect AQ-CO-HOMENET
* Hayward Goldline AQL2-BASE-RF AquaConnect Wireless Antenna

I also have a Hubitat C8 Hub and use the Google Home app with the Google Home Community app.

I added a rule on my Firewalla router to block the Hayward AquaConnect from communicating with the cloud service due to security concerns.  I tried to contact Hayward on 2 separate occassions about my concerns, but they refused to call me back.

This app allows the Hubitat to communicate directly to the Hayward AquaConnect device over the local network without the need to communicate over the Internet.  Note that you will need to configure a static IP for the Hayward AquaConnect, otherwise this app will break everytime an IP address is reassigned.

Note this app is in BETA release.  It works for my use-case, but your configuration may differ.  Of particular importance is the use of the Heater.  I take no liability if your heater turns on unexpectedly and runs up your Gas or Electric bill.

One of the items I wanted to fix with this interface is how the Heater turns on and off.  In the default setup provided by Hayward, I have to rotate the temperature from Off to 104 down to the number I want to set it on every time I turn the heater on (and vice-versa to turn it off).  This interface is extermely cumbersome.  For my hubitat interface, you need to configure the heatingSetPoint to the temperature you desire when the heater is on.  The on/off switch is then a one-time action where the code takes care of rotating through the range of temperatures.  In this way you only need to configure the heatingSetPoint once, and then use the On/Off switch (single button press) and the code will take care of driving through the menu selections and the temperature rotations (usually in excess of 20 button presses without my interface -- very poor design).
