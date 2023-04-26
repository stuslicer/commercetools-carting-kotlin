package com.kingfisher.kotlinbasketdemo.service

import com.commercetools.api.models.cart.Cart
import com.commercetools.api.models.cart.LineItem

interface CartService {

    /**
     * Get a cart with the given id.
     * @param cartId
     * @return
     */
    fun getCartById(cartId: String): Cart
    fun getCartByIdAsString(cartId: String, prettyPrint: Boolean = true): String

    fun createCart() : Cart

    /**
     * Add an item to the cart with the quantity.
     * Variant is required because possible to add the same product with different variants.
     * @param cartId
     * @param cartVersion
     * @param productId
     * @param variant
     * @param quantity
     * @return
     */
    fun addItemToCart(cartId: String, cartVersion: Long, productId: String, variant: Long, quantity: Long): Cart

//    fun addItemToCart(cartId: String, cartVersion: Long, productId: String, variant: Long, quantity: Long,
//                      fulfilmentOption: FulfilmentOption): Cart

    /**
     * Remove an item the cart, effectively reduces the number of items by given quantity.
     * If quantity is zero then removes the entire item line.
     * @param cartId
     * @param cartVersion
     * @param productId
     * @param variant
     * @param quantity
     * @return
     */
    fun removeItemFromCart(cartId: String, cartVersion: Long, productId: String, variant: Long, quantity: Long): Cart

    /**
     * Sets the quantity for the given cart line item.
     * The line item has to exist.
     * @param cartId
     * @param cartVersion
     * @param lineItemId
     * @param quantity
     * @return
     */
    fun setItemQuantityInCart(cartId: String, cartVersion: Long, lineItemId: String, quantity: Long): Cart

    /**
     * Gets the cart line item for given product and variant.
     * @param cartId
     * @param productId
     * @param variantId
     * @return
     */
    fun getItemFromCart(cartId: String, productId: String, variantId: Long): LineItem?

    fun addShippingAddress(
        cartId: String, cartVersion: Long,
        key: String, firstName: String?, lastName: String?,
        streetNumber: String?, streetName: String?, city: String?, postalCode: String?, country: String?
    ): Cart

    /**
     * Print out the cart in all its glory...
     * @param cart
     */
    fun printCart(cart: Cart)
}