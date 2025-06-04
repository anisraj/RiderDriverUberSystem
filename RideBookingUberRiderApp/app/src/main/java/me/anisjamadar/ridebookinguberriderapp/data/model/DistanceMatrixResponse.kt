package me.anisjamadar.ridebookinguberriderapp.data.model

import com.google.gson.annotations.SerializedName

data class DistanceMatrixResponse(
    @SerializedName("rows")
    val rows: List<Row>?
) {
    data class Row(
        @SerializedName("elements")
        val elements: List<Element>?
    ) {
        data class Element(
            @SerializedName("distance")
            val distance: Double?
        )
    }
}
