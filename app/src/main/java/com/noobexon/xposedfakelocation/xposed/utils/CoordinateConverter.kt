package com.noobexon.xposedfakelocation.xposed.utils

import kotlin.math.*

object CoordinateConverter {
    private const val PI = 3.1415926535897932384626
    private const val A = 6378245.0
    private const val EE = 0.00669342162296594323

    private fun transformLat(x: Double, y: Double): Double {
        var ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(y * PI) + 40.0 * sin(y / 3.0 * PI)) * 2.0 / 3.0
        ret += (160.0 * sin(y / 12.0 * PI) + 320 * sin(y * PI / 30.0)) * 2.0 / 3.0
        return ret
    }

    private fun transformLon(x: Double, y: Double): Double {
        var ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * sqrt(abs(x))
        ret += (20.0 * sin(6.0 * x * PI) + 20.0 * sin(2.0 * x * PI)) * 2.0 / 3.0
        ret += (20.0 * sin(x * PI) + 40.0 * sin(x / 3.0 * PI)) * 2.0 / 3.0
        ret += (150.0 * sin(x / 12.0 * PI) + 300.0 * sin(x / 30.0 * PI)) * 2.0 / 3.0
        return ret
    }

    fun outOfChina(lat: Double, lon: Double): Boolean {
        if (lon < 72.004 || lon > 137.8347) return true
        if (lat < 0.8293 || lat > 55.8271) return true
        return false
    }

    fun wgs84ToGcj02(lat: Double, lon: Double): Pair<Double, Double> {
        if (outOfChina(lat, lon)) return Pair(lat, lon)
        var dLat = transformLat(lon - 105.0, lat - 35.0)
        var dLon = transformLon(lon - 105.0, lat - 35.0)
        val radLat = lat / 180.0 * PI
        var magic = sin(radLat)
        magic = 1 - EE * magic * magic
        val sqrtMagic = sqrt(magic)
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI)
        dLon = (dLon * 180.0) / (A / sqrtMagic * cos(radLat) * PI)
        return Pair(lat + dLat, lon + dLon)
    }

    fun gcj02ToWgs84(lat: Double, lon: Double): Pair<Double, Double> {
        val (gcjLat, gcjLon) = wgs84ToGcj02(lat, lon)
        return Pair(lat * 2 - gcjLat, lon * 2 - gcjLon)
    }

    fun gcj02ToBd09(lat: Double, lon: Double): Pair<Double, Double> {
        val z = sqrt(lon * lon + lat * lat) + 0.00002 * sin(lat * PI)
        val theta = atan2(lat, lon) + 0.000003 * cos(lon * PI)
        val bdLon = z * cos(theta) + 0.0065
        val bdLat = z * sin(theta) + 0.006
        return Pair(bdLat, bdLon)
    }

    fun bd09ToGcj02(lat: Double, lon: Double): Pair<Double, Double> {
        val x = lon - 0.0065
        val y = lat - 0.006
        val z = sqrt(x * x + y * y) - 0.00002 * sin(y * PI)
        val theta = atan2(y, x) - 0.000003 * cos(x * PI)
        val gcjLon = z * cos(theta)
        val gcjLat = z * sin(theta)
        return Pair(gcjLat, gcjLon)
    }

    fun wgs84ToBd09(lat: Double, lon: Double): Pair<Double, Double> {
        val (gcjLat, gcjLon) = wgs84ToGcj02(lat, lon)
        return gcj02ToBd09(gcjLat, gcjLon)
    }

    fun bd09ToWgs84(lat: Double, lon: Double): Pair<Double, Double> {
        val (gcjLat, gcjLon) = bd09ToGcj02(lat, lon)
        return gcj02ToWgs84(gcjLat, gcjLon)
    }

    enum class CoordinateSystem {
        WGS84, GCJ02, BD09
    }

    fun convertFromWgs84(lat: Double, lon: Double, targetSystem: CoordinateSystem): Pair<Double, Double> {
        return when (targetSystem) {
            CoordinateSystem.WGS84 -> Pair(lat, lon)
            CoordinateSystem.GCJ02 -> wgs84ToGcj02(lat, lon)
            CoordinateSystem.BD09 -> wgs84ToBd09(lat, lon)
        }
    }
}
