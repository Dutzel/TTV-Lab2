#!/bin/bash

usage(){
	echo -e "\n*****************************usage*****************************"
	echo -e "$0 <test> <bootstrap-ip-address:bootstrap-port> <players-ip-address> <players-port> <number-of-test-threads> <name-of-strategy> <coap-url:coap-port>"
	echo -e "$0 <contest> <bootstrap-ip-address:bootstrap-port> <players-ip-address> <players-port> <create | join> <name-of-strategy> <coap-url:coap-port>"
	echo -e "*****************************usage*****************************\n"
	
	echo -e "*****************************examples*****************************"
	echo -e "Tests from root of this project:"
	echo -e "(1) run 4 players (one creats a network; three join it) in one console with StrategyOne:\n      $0 test localhost:10000 localhost 10001 4 app.StrategyOne localhost:5683"
	echo -e "(2.0) run 1 player in one console with StrategyOne which creates a network on localhost:10000 (<players-ip-address> and <players-port> are ignored):\n      $0 contest localhost:10000 localhost 0 create app.StrategyOne localhost:5683"
	echo -e "(2.1) run 1 player in one console with StrategyOne which joins a network running on localhost:10000:\n      $0 contest localhost:10000 localhost 10001 join app.StrategyOne localhost:5683"
	echo -e "*****************************examples*****************************\n"
	
}
if [ $# -eq 7 ]
then
		/usr/bin/java -Xms2048m -Xmx2048m -cp "bin:libs/californium_core_all.jar:libs/log4j-1.2.17.jar:properties/chord.properties:properties/log4j.properties"  app.test.RunBattleShips $* 
else
		usage
fi
