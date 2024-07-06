package com.lulan.shincolle.team;

import com.lulan.shincolle.utility.LogHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Fleet Data
 * <p>
 * a team = a fleet with one leader
 * team ally = friendly team, not ally = hostile team
 * team banned = always hostile team, you can't ally with banned team
 */
public class TeamData {

    protected int teamID;                    //team id = player UID
    protected String teamName;                //team name
    protected String leaderName;            //team leader name
    protected List<Integer> teamBanID;        //team banned id list
    protected List<Integer> teamAllyID;        //team ally id list


    public TeamData() {
        this.teamID = 0;
        this.teamName = "   ";
        this.leaderName = "   ";
        this.teamBanID = new ArrayList();
        this.teamAllyID = new ArrayList();
    }

    public TeamData(int teamID, String teamName, String leaderName) {
        this.teamID = teamID;
        this.teamName = teamName;
        this.leaderName = leaderName;
        this.teamBanID = new ArrayList();
        this.teamAllyID = new ArrayList();
    }

    /**
     * getter
     */
    public String getTeamName() {
        return this.teamName;
    }

    /**
     * setter
     */
    public void setTeamName(String par1) {
        this.teamName = par1;
    }

    public String getTeamLeaderName() {
        return this.leaderName;
    }

    public void setTeamLeaderName(String par1) {
        this.leaderName = par1;
    }

    public int getTeamID() {
        return this.teamID;
    }

    public void setTeamID(int par1) {
        this.teamID = par1;
    }

    public List<Integer> getTeamBannedList() {
        if (this.teamBanID == null) {
            this.teamBanID = new ArrayList();
        }

        return this.teamBanID;
    }

    public void setTeamBannedList(List<Integer> par1) {
        this.teamBanID = par1;
    }

    public List<Integer> getTeamAllyList() {
        if (this.teamAllyID == null) {
            this.teamAllyID = new ArrayList();
        }

        return this.teamAllyID;
    }

    public void setTeamAllyList(List<Integer> par1) {
        this.teamAllyID = par1;
    }

    //add ally
    public void addTeamAlly(int par1) {
        if (par1 > 0) {
            if (this.teamAllyID != null) {
                //not ally and not banned team => add ally
                if (!this.teamAllyID.contains(par1) && !this.teamBanID.contains(par1)) {
                    this.teamAllyID.add(par1);
                    LogHelper.debug("DEBUG: team data: add ally: team " + this.teamName + " add " + par1);
                }
            }
        }
    }

    //remove ally
    public void removeTeamAlly(int par1) {
        if (par1 > 0) {
            if (this.teamAllyID != null) {
                //existed, remove ally
                if (this.teamAllyID.contains(par1)) {
                    this.teamAllyID.remove((Integer) par1);  //remove object, not index
                    LogHelper.debug("DEBUG : team data: remove ally: team " + this.teamName + " remove " + par1);
                }
            }
        }
    }

    //add Banned (hostile) team
    public void addTeamBanned(int par1) {
        if (par1 > 0) {
            if (this.teamBanID != null) {
                //not existed, add it
                if (!this.teamBanID.contains(par1) && !this.teamAllyID.contains(par1)) {
                    this.teamBanID.add(par1);
                    LogHelper.debug("DEBUG: team data: add banned: team " + this.teamName + " add " + par1);
                }
            }
        }
    }

    //remove Banned (hostile) team
    public void removeTeamBanned(int par1) {
        if (par1 > 0) {
            if (this.teamBanID != null) {
                //existed, remove it
                if (this.teamBanID.contains(par1)) {
                    this.teamBanID.remove((Integer) par1);  //remove object, not index
                    LogHelper.debug("DEBUG: team data: remove banned: team " + this.teamName + " remove " + par1);
                }
            }
        }
    }

    //check team is in ally list
    public boolean isTeamAlly(int par1) {
        //id 0 = always friendly
        if (par1 == 0) return true;

        if (par1 > 0 && this.teamAllyID != null) {
            return this.teamAllyID.contains(par1);
        }

        return false;
    }

    //check team is in ban list
    public boolean isTeamBanned(int par1) {
        //id 0 = always friendly
        if (par1 == 0) return false;

        if (par1 > 0 && this.teamBanID != null) {
            return this.teamBanID.contains(par1);
        }

        return false;
    }


}
