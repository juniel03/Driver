package com.bluesolution.driver.data.models

data class Order(
    val accepted: Boolean,
    val orderPickedUp: Boolean,
    val pathToUser: String,
    val arrived: Boolean,
    val deliveryAddress: Coordinates,
    val storeAddress: Coordinates,
    val driverLocation: Coordinates
)
