/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package coolmap.application.plugin;

import net.xeoh.plugins.base.Plugin;
import org.json.JSONObject;

/**
 *
 * @author sugang
 */
public interface CoolMapPlugin extends Plugin {
    
    public void initialize(JSONObject pluginConfig);
    public String getName();
}
