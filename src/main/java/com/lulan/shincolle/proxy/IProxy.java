package com.lulan.shincolle.proxy;

public interface IProxy {

    //key binding
    void registerKeyBindings();

    //render
    void registerRender() throws Exception;

    //packet channel
    void registerChannel();

    //capability
    void registerCapability();


}
