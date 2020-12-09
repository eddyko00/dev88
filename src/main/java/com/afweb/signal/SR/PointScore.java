/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.afweb.signal.SR;

import java.util.List;

/**
 *
 * @author koed
 */
public class PointScore {
    private Double point;
    private Double score;
    private List<PointEvent> pointEventList;

    public PointScore(Double point, Double score, List<PointEvent> pointEventList) {
        this.point = point;
        this.score = score;
        this.pointEventList = pointEventList;
    }

    /**
     * @return the point
     */
    public Double getPoint() {
        return point;
    }

    /**
     * @param point the point to set
     */
    public void setPoint(Double point) {
        this.point = point;
    }

    /**
     * @return the score
     */
    public Double getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(Double score) {
        this.score = score;
    }

    /**
     * @return the pointEventList
     */
    public List<PointEvent> getPointEventList() {
        return pointEventList;
    }

    /**
     * @param pointEventList the pointEventList to set
     */
    public void setPointEventList(List<PointEvent> pointEventList) {
        this.pointEventList = pointEventList;
    }

}