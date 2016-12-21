/*******************************************************************************
 * Copyright (c) 2015,  Stefan Andres.  All rights reserved.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 3.0 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 *  
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *******************************************************************************/
package de.myandres.optolink;

/*
 * Install a Socked Handler for ip communication 
 * 
 * Server can found via Broadcast
 * Server API Client can connect via TCP
 * 
 */
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class MyHttpServer  {

	static Logger log = LoggerFactory.getLogger(MyHttpServer.class);

	private Config config;
	private HttpServer server;
	private ViessmannHandler viessmannHandler;
	private PrintStream out;
	

	MyHttpServer(Config config, ViessmannHandler viessmannHandler) throws Exception {
		this.config = config;
		this.viessmannHandler = viessmannHandler;
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(config.getPort()+1), 0);
			List<Thing> thingList = config.getThingList();
			for (int i = 0; i < thingList.size(); i++) {
				Thing thing = thingList.get(i);
				List<Channel> channelMap = thing.getChannelMap();
				for (int j = 0; j < channelMap.size(); j++) {
					String path = "/get_" + thing.getId() + "_" + channelMap.get(j).getId();
					log.info("Registering endpoint {}", path);
    				server.createContext(path , new MyHandler(thing.getId(), channelMap.get(j).getId(), this.config, this.viessmannHandler));

				}
			}
	        server.setExecutor(null); // creates a default executor
	        server.start();
	        log.info("Http Server started on port {}", config.getPort()+1);
	    }
        catch (Exception e) {
      	        log.info("Exception : {}", e.getMessage());
        } 
	}

}

class MyHandler implements HttpHandler {

		static Logger log = LoggerFactory.getLogger(MyHandler.class);

		private Config config;
		private ViessmannHandler viessmannHandler;
		private PrintStream out;
		private String thingId;
		private String channelId;


		MyHandler(String thing_id, String channel_id, Config config, ViessmannHandler viessmannHandler) throws Exception {
			this.config = config;
			this.viessmannHandler = viessmannHandler;
			this.thingId = thing_id;
			this.channelId = channel_id;
		}


        @Override
        public void handle(HttpExchange t) throws IOException {
        	log.debug("Http Handler called");
            log.debug("Try to get Thing for ID: {}", thingId);
			Thing thing = config.getThing(thingId);
			String response = null;
			if (thing != null) {
				log.debug("Try to get Channel for ID: {}", channelId);
				Channel chan = thing.getChannel(channelId);
				response = viessmannHandler.getValue(chan.getTelegram());
			}
            t.sendResponseHeaders(200, response.length());
            OutputStream os = t.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

}
