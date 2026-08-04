package com.mijingxingzuo.app;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "RiskCheck")
public class RiskCheckPlugin extends Plugin {
    
    @PluginMethod
    public void startRiskCheck(PluginCall call) {
        try {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                activity.performRiskCheckFromFrontend();
                JSObject ret = new JSObject();
                ret.put("success", true);
                call.resolve(ret);
            } else {
                JSObject ret = new JSObject();
                ret.put("success", false);
                ret.put("message", "Activity not found");
                call.reject("Activity not found");
            }
        } catch (Exception e) {
            JSObject ret = new JSObject();
            ret.put("success", false);
            ret.put("message", e.getMessage());
            call.reject(e.getMessage());
        }
    }
    
    @PluginMethod
    public void getRiskStatus(PluginCall call) {
        try {
            MainActivity activity = (MainActivity) getActivity();
            if (activity != null) {
                RiskDetector.RiskResult result = RiskDetector.checkAllRisks(activity);
                JSObject ret = new JSObject();
                ret.put("hasRisk", result.hasRisk);
                ret.put("riskDescription", result.riskDescription);
                call.resolve(ret);
            } else {
                call.reject("Activity not found");
            }
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }
}
