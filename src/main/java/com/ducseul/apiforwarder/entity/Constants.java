package com.ducseul.apiforwarder.entity;

public class Constants {
    public enum API_MODE {
        FORWARD("forward"), MOCK("mock"), EVAL("eval");
        private String value;
        API_MODE(String value) {
            this.value = value;
        }
        public static API_MODE from(String s){
            return API_MODE.valueOf(s.toUpperCase());
        }
    }

    public enum METHOD {
        GET, POST;

        public static METHOD getMethod(String method){
            switch (method.toLowerCase()){
                case "get": return GET;
                case "post": return POST;
                default: return POST;
            }
        }
    }
}
