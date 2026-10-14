package com.lulan.shincolle;

/**
 * Build-time constants. Generated from src/main/java-templates by Blossom.
 * Replaces the RetroFuturaGradle injectedTags (formerly tags.properties); field names are unchanged.
 */
public class Tags {
    private Tags() {}

    public static final String VERSION = "{{ mod_version }}";
    public static final String MOD_ID = "{{ mod_id }}";
    public static final String MOD_NAME = "{{ mod_name }}";

    public static final String CLIENT_PROXY = "com.lulan.{{ mod_id }}.proxy.ClientProxy";
    public static final String SERVER_PROXY = "com.lulan.{{ mod_id }}.proxy.ServerProxy";
    public static final String TEXTURES_BLOCKS = "{{ mod_id }}:textures/blocks/";
    public static final String TEXTURES_ENTITY = "{{ mod_id }}:textures/entity/";
    public static final String TEXTURES_GUI = "{{ mod_id }}:textures/gui/";
    public static final String TEXTURES_PARTICLE = "{{ mod_id }}:textures/particles/";
    public static final String MOD_ID_Baubles = "baubles";
    public static final String MOD_ID_IC2 = "ic2";
    public static final String MOD_ID_Metamorph = "metamorph";
}
