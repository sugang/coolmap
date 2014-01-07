/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package coolmap.application.utils;

/**
 *
 * @author sugang
 */
public class Session {

    private String sessionName = null;
    private String sessionURI = null;
    
    public Session(String name, String uri){
                
        sessionName = name;
        sessionURI = uri;
    }

    public String getSessionName() {
        return sessionName;
    }


    public String getSessionURI() {
        return sessionURI;
    }

    public void setSessionURI(String sessionURI) {
        this.sessionURI = sessionURI;
    }

}
