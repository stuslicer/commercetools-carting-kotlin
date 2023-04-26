package com.kingfisher.kotlinbasketdemo

import com.commercetools.api.client.ProjectApiRoot
import com.commercetools.api.models.cart.Cart
import com.kingfisher.kotlinbasketdemo.service.CtCartService
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
	val cartService: CtCartService
): CommandLineRunner {

	override fun run(vararg args: String?) {
		println("Ta da!")

		val productById = productService.getProductById("64544219-d6cb-4680-ad20-b09ce0a5c088")
		println(productById)

		var cart: Cart

		// 1. Create Cart
		val CART_ID: String = "1bb01b25-b961-422a-915e-e6a85f5a38da"

		// 2. Select cart
		cart  = cartService.getCartById( CART_ID )
		var cartVersion: Long = cart?.version ?: 0

		println( cart )
		cartService.printCart(cart)

		println(cartService.getCartByIdAsString(CART_ID, false))

	}

}