package com.example.fastroutes.utils

import com.google.android.gms.maps.model.LatLng

object PolylineDecoder {

    fun decode(encoded: String): List<LatLng> {
        val polyline = mutableListOf<LatLng>()

        var index = 0
        val length = encoded.length
        var latitude = 0
        var longitude = 0

        while (index < length) {
            var result = 0
            var shift = 0
            var byte: Int

            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)

            val deltaLatitude = if ((result and 1) != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }

            latitude += deltaLatitude

            result = 0
            shift = 0

            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)

            val deltaLongitude = if ((result and 1) != 0) {
                (result shr 1).inv()
            } else {
                result shr 1
            }

            longitude += deltaLongitude

            polyline.add(
                LatLng(
                    latitude / 100000.0,
                    longitude / 100000.0
                )
            )
        }

        return polyline
    }
}