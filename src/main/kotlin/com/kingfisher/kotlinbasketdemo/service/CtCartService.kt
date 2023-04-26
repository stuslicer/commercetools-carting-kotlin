package com.kingfisher.kotlinbasketdemo.service

import com.commercetools.api.client.ProjectApiRoot
import com.commercetools.api.models.cart.*
import com.commercetools.api.models.common.AddressDraftBuilder
import com.commercetools.api.models.shipping_method.ShippingMethodResourceIdentifierBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.kingfisher.kotlinbasketdemo.exception.InvalidCartId
import com.kingfisher.kotlinbasketdemo.utils.printLocalized
import com.kingfisher.kotlinbasketdemo.utils.printPrice
import io.vrap.rmf.base.client.error.NotFoundException
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CtCartService(
    val apiRoot: ProjectApiRoot,
    val productService: ProductService,
    val objectMapper: ObjectMapper
) : CartService {

    override fun getCartById(cartId: String): Cart {
        return try {
            apiRoot.carts()
                .withId(cartId)
                .get()
                .executeBlocking()
                .body;
        } catch ( e: NotFoundException) {
            throw InvalidCartId("Cart id ${cartId} is invalid.")
        }
    }

    override fun getCartByIdAsString(cartId: String, prettyPrint: Boolean): String {
        val body = try {
            apiRoot.carts()
                .withId(cartId)
                .get()
                .sendBlocking()
                .body;
        } catch ( e: NotFoundException) {
            return ""
        }

        return if( prettyPrint ) {
            val asObject: Any = objectMapper.readValue<Any>(body, Any::class.java)
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(asObject)

        } else {
            String(body)
        }
    }

    override fun createCart() : Cart {
        val cartDraft = CartDraftBuilder.of()
            .currency("GBP")
            .country("GB")
            .locale("en-GB")
            .shippingMode( ShippingMode.MULTIPLE )
            .taxRoundingMode( RoundingMode.HALF_DOWN )
            .build();

        val cart = apiRoot.carts()
            .post( cartDraft )
            .executeBlocking()
            .body;

        return cart
    }

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
    override fun addItemToCart(cartId: String, cartVersion: Long, productId: String, variant: Long, quantity: Long): Cart {
        val cartUpdate = CartUpdateBuilder.of()
            .version(cartVersion)
            .plusActions { actionBuilder: CartUpdateActionBuilder ->
                actionBuilder.addLineItemBuilder()
                    .productId(productId)
                    .variantId(variant)
                    .quantity(quantity)
            }
            .build()
        return performUpdateCart(cartId, cartUpdate)
    }

    fun addItemToCart(cartId: String, cartVersion: Long, productId: String, variant: Long, quantity: Long,
                      fulfilmentOption: FulfilmentOption = FulfilmentOption.DELIVERY,
                      address: Address? = null): Cart {

        val whatIsThis: CartAddLineItemAction? = CartUpdateActionBuilder.of()
            .addLineItemBuilder()
            .productId(productId)
            .variantId(variant)
            .quantity(quantity)
            .build()

        val addressKey = when( fulfilmentOption ) {
            FulfilmentOption.DELIVERY, FulfilmentOption.DSV -> "USER_DELIVERY"
            else -> "STORE_ADDRESS"
        }

        // 1. At cart level add the address with the appropriate key
        //       addShippingItemAddress - https://docs.commercetools.com/api/projects/carts#add-itemshippingaddress
        //
        // 2. At the cart level add the shipping method with the appropriate key
        //        addShippingMwthod - https://docs.commercetools.com/api/projects/carts#add-shippingmethod
        val shippingMethodIdRef = ShippingMethodResourceIdentifierBuilder.of()
            .id("")
            .build()
        val addShippingMethodAction = CartAddShippingMethodActionBuilder.of()
//            .shippingAddress( address ) // build a BaseAddress from local Address
            .shippingKey(fulfilmentOption.toString())
            .shippingMethod(shippingMethodIdRef)
            .build()
        //
        // 3. At the line item level add the shipping details - which must include the shipping method, the address and the quantity

        // one per fulfilment option
        val shippingTargetHome = ItemShippingTargetBuilder.of()
            .quantity(2)
            .addressKey("HOME")
            .shippingMethodKey(fulfilmentOption.toString())
            .build()

        // add or plus a target for each fulfilment option
        val shippingDetailsDraft = ItemShippingDetailsDraftBuilder.of()
            .targets(shippingTargetHome)
            .build()

        val setLineItemAction = CartSetLineItemShippingDetailsActionBuilder.of()
            .lineItemId("")
            .shippingDetails(shippingDetailsDraft)
            .build()


        val cartUpdate = CartUpdateBuilder.of()
            .version(cartVersion)
            .plusActions( whatIsThis, addShippingMethodAction, setLineItemAction )
            .build()
        return performUpdateCart(cartId, cartUpdate)
    }

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
    override fun removeItemFromCart(cartId: String, cartVersion: Long, productId: String, variant: Long, quantity: Long): Cart {
        val itemFromCart: LineItem? = getItemFromCart(cartId, productId, variant)
        if (itemFromCart  != null) {
            val cartUpdate = CartUpdateBuilder.of()
                .version(cartVersion)
                .plusActions { actionBuilder: CartUpdateActionBuilder ->
                    if (quantity == 0L) {
                        actionBuilder.removeLineItemBuilder()
                            .lineItemId(itemFromCart.id)
                    } else {
                        actionBuilder.removeLineItemBuilder()
                            .lineItemId(itemFromCart.id)
                            .quantity(quantity)
                    }
                }
                .build()
            return performUpdateCart(cartId, cartUpdate)
        }
        return getCartById(cartId)
    }

    /**
     * Sets the quantity for the given cart line item.
     * The line item has to exist.
     * @param cartId
     * @param cartVersion
     * @param lineItemId
     * @param quantity
     * @return
     */
    override fun setItemQuantityInCart(cartId: String, cartVersion: Long, lineItemId: String, quantity: Long): Cart {
        val cartUpdate = CartUpdateBuilder.of()
            .version(cartVersion)
            .plusActions { actionBuilder ->
                actionBuilder.changeLineItemQuantityBuilder()
                    .lineItemId(lineItemId)
                    .quantity(quantity)
            }
            .build()
        return performUpdateCart(cartId, cartUpdate)
    }

    /**
     * Gets the cart line item for given product and variant.
     * @param cartId
     * @param productId
     * @param variantId
     * @return
     */
    override fun getItemFromCart(cartId: String, productId: String, variantId: Long): LineItem? {

        // validate
        val body = apiRoot.carts()
            .withId(cartId)
            .get()
            .executeBlocking()
            .body
        val candidates = body.lineItems.stream()
            .filter { item: LineItem ->
                item.productId == productId && item.variant.id == variantId
            }
            .toList()

        // MAY NEED TO LOOK AT SHPPING METHOD ETC
        return if (candidates.isEmpty()) null else candidates[0]
    }

    override fun addShippingAddress(
        cartId: String, cartVersion: Long,
        key: String, firstName: String?, lastName: String?,
        streetNumber: String?, streetName: String?, city: String?, postalCode: String?, country: String?
    ): Cart {
        val draft = AddressDraftBuilder.of()
            .key(key)
            .firstName(firstName)
            .lastName(lastName)
            .streetNumber(streetNumber)
            .streetName(streetName)
            .city(city)
            .postalCode(postalCode)
            .country(country)
            .build()
        val cartUpdate = CartUpdateBuilder.of()
            .version(cartVersion)
            .plusActions { actionBuilder: CartUpdateActionBuilder ->
                actionBuilder.setShippingAddressBuilder()
                    .address(draft)
            }
            .build()
        return performUpdateCart(cartId, cartUpdate)
    }

    /**
     * Print out the cart in all its glory...
     * @param cart
     */
    override fun printCart(cart: Cart) {
        val lineItems = cart.lineItems
        println("Cart id: ${cart.id} - ${cart.version}")
        println("State: ${cart.cartState}")
        println("Items: $cart.lineItems.size, total: $cart.totalLineItemQuantity")
        println("Total price: ${printPrice(cart.totalPrice)}" )
        println("Tax price: ${printPrice(cart.taxedPrice)}" )
        println("Shipping mode: ${cart.shippingMode}" )
        for (lineItem in lineItems) {
            val product = productService.getProductById(lineItem.productId)

            if( product != null ) {
                println(
                    "${product.id} ${printLocalized(product.masterData.current.name)} - ${product.key} - ${lineItem.quantity} ${printPrice(lineItem.totalPrice)} ${printPrice(lineItem.taxedPrice)}  "
                )
            }

        }
    }

    private fun performUpdateCart(cartId: String, cartUpdate: CartUpdate): Cart {
        return apiRoot.carts()
            .withId( cartId )
            .post(cartUpdate )
            .executeBlocking( Duration.ofMillis( 2_000 ) )
            .body;
    }

}