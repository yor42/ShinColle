package com.lulan.shincolle.entity;

import com.lulan.shincolle.reference.unitclass.Attrs;

/**
 * attributes: hp, def, atk, cri, ...
 */
public interface IShipAttrs {

    /**
     * attributes like HP, DEF, ATK...
     */
    Attrs getAttrs();

    void setAttrs(Attrs data);


}