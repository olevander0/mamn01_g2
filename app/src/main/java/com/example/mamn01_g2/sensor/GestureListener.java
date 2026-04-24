package com.example.mamn01_g2.sensor;

public interface GestureListener {

    void onTimeRotate(int minutesToChange);


    void onPhoneFlippedDown();


    void onPhoneFlippedUp();


    void onPhoneLiftedFaceDown();
}
