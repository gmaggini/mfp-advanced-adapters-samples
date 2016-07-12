package com.github.mfpdev.sample.socialogin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.mfp.server.registration.external.model.AuthenticatedUser;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by NL54154 on 07/07/16.
 */
public class LinkedinSupport implements LoginVendor {


    private static final Logger logger = Logger.getLogger(FacebookSupport.class.getName());

    private ObjectMapper mapper = new ObjectMapper();
    private SSLSocketFactory sslSocketFactory;

    @Override
    public String[] getConfigurationPropertyNames() {
        return new String[0];
    }

    @Override
    public void setConfiguration(Properties properties, SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;

    }

    @Override
    public boolean isEnabled() {
        return true;  //return true to enable support for this vendor
    }

    @Override
    public AuthenticatedUser validateTokenAndCreateUser(String tokenStr, String checkName) {
        HttpsURLConnection connection = null;
        String error;
        try {
            String req = "https://api.linkedin.com/v1/people/~:(firstName,lastName,headline,email-address,id,num-connections,picture-url)?format=json&oauth2_access_token=" + tokenStr;
            connection = (HttpsURLConnection) new URL(req).openConnection();
            connection.setSSLSocketFactory(sslSocketFactory);
            connection.setRequestMethod("GET");
            connection.connect();
            int responseCode = connection.getResponseCode();
            String content = readContent(responseCode == 200 ? connection.getInputStream() : connection.getErrorStream());

            if (responseCode == 200) {
                Map data = mapper.readValue(content, Map.class);
                HashMap<String,Object> userAttributes = new HashMap<>();
                for (Object key : data.keySet()) {
                    userAttributes.put((String)key, data.get(key));
                }
                return new AuthenticatedUser((String) data.get("id"), (String) data.get("firstName") + " " + (String) data.get("lastName"), checkName, userAttributes);
            } else {
                error = content;
            }
        } catch (Exception e) {
            error = e.toString();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        logger.severe("Failed to validate LinkedIn access token: " + error);
        return null;
    }


    private String readContent(InputStream inputStream) throws IOException {
        String content = "";
        BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = in.readLine()) != null) {
            content += line + "\n";
        }
        in.close();
        return content;
    }
}
