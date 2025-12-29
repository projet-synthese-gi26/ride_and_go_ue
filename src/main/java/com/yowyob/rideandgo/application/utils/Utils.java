package com.yowyob.rideandgo.application.utils;

import java.util.UUID;

public class Utils {
    public static UUID generateUUID(){
        return UUID.randomUUID();
    }

    public static int generateRandomNumber(){
        return (int) (Math.random()*100);
    }

    public static int generateRandomNumber(int max){
        return (int) (Math.random()*max);
    }
}