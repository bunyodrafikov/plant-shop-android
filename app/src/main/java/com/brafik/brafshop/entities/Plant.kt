package com.brafik.brafshop.entities

data class Plant(
    val price: Long,
    val name: String,
    val description: String,
    val pots: List<Long>,
    val id: Long,
    val category: String
) {
    override fun toString(): String {
        return "Plant(id=$id, name='$name', price=$price, category='$category', description='$description', pots=$pots)"
    }
}