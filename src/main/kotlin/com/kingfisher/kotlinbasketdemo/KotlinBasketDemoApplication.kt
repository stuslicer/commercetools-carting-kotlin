package com.kingfisher.kotlinbasketdemo

import com.commercetools.api.client.ProjectApiRoot
import com.commercetools.api.models.cart.Cart
import com.kingfisher.kotlinbasketdemo.data.DataStore
import com.kingfisher.kotlinbasketdemo.exception.InvalidProductId
import com.kingfisher.kotlinbasketdemo.service.CtCartService
import com.kingfisher.kotlinbasketdemo.service.FulfilmentOption
import com.kingfisher.kotlinbasketdemo.service.ProductService
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.stereotype.Component

@SpringBootApplication
@ConfigurationPropertiesScan(value = ["com.kingfisher.kotlinbasketdemo.config"])
class KotlinBasketDemoApplication

fun main(args: Array<String>) {
	runApplication<KotlinBasketDemoApplication>(*args)
}

@Component
class Runner(
	val apiRoot: ProjectApiRoot,
	val productService: ProductService,
	val cartService: CtCartService,
	val dataStore: DataStore
): CommandLineRunner {

	/**
	 * Create a new cart or load an existing one
	 */
	fun createCart(cartId: String = ""): Cart {
		val cart = if( cartId != "" ) {
			cartService.getCartById(cartId)
		} else {
			cartService.createCart()
		}
		println(cart.id)
		cartService.printCart( cart )
		return cart
	}

	override fun run(vararg args: String?) {
		println("Ta da!")

		val productById = try {
			productService.getProductById("64544219-d6cb-4680-ad20-b09ce0a5c088")
		} catch ( e: InvalidProductId) {
			null
		}
		println(productById)

//		val productProjectionsWithTaxCategory = productService.findProductProjectionsWithTaxCategory()
//		println(productProjectionsWithTaxCategory)

		val PRODUCT_1 = dataStore.projects()[0]
		val PRODUCT_2 = dataStore.projects()[1]
		val PRODUCT_3 = dataStore.projects()[3]


		var cart: Cart

		// 1. Create Cart
		cart = createCart("106fa62b-ebe6-429c-9400-b9a73c0f0249")

		val CART_ID: String = cart.id
//		val CART_ID: String = "507185c3-f493-4a5c-b205-a20361dcc694"

		// 2. Select cart
		cart  = cartService.getCartById( CART_ID )
		var cartVersion: Long = cart.version

		println( cart )
		cartService.printCart(cart)

		// 3. Add item to cart
//        cart = cartService.addItemToCart( CART_ID, cartVersion, PRODUCT_1.id, PRODUCT_1.variant, 2 );
//		cartVersion = cart.version

		// 4. Add item to cart with a fulfilment option
//        cart = cartService.addItemToCart( CART_ID, cartVersion, PRODUCT_2.id, PRODUCT_2.variant, 1, FulfilmentOption.DELIVERY, dataStore.addresses()[0] );
//		cartVersion = cart.version

		// remove item from cart - to start again ...
//		cart = cartService.removeItemFromCart( CART_ID, cartVersion,PRODUCT_2.id, PRODUCT_2.variant, 0 )
//		cartVersion = cart.version


		println( cart )
		cartService.printCart(cart)

		println(cartService.getCartByIdAsString(CART_ID, false))

		// X. Delete if required
//		val deletedCart = cartService.deleteCart( CART_ID, cartVersion )
//		println("Cart deleted!")

	}

}