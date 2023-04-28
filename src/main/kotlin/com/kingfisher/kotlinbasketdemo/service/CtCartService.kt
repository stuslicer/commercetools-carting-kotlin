package com.kingfisher.kotlinbasketdemo.service

import com.commercetools.api.client.ProjectApiRoot
import com.commercetools.api.models.cart.*
import com.commercetools.api.models.common.AddressBuilder
import com.commercetools.api.models.common.AddressDraftBuilder
import com.commercetools.api.models.shipping_method.ShippingMethodResourceIdentifier
import com.commercetools.api.models.shipping_method.ShippingMethodResourceIdentifierBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.kingfisher.kotlinbasketdemo.data.Address
import com.kingfisher.kotlinbasketdemo.data.DataStore
import com.kingfisher.kotlinbasketdemo.data.FulfilmentOption
import com.kingfisher.kotlinbasketdemo.exception.InvalidCartId
import com.kingfisher.kotlinbasketdemo.utils.*
import io.vrap.rmf.base.client.error.NotFoundException
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CtCartService(
    val apiRoot: ProjectApiRoot,
    val productService: ProductService,
    val dataStore: DataStore,
    val objectMapper: ObjectMapper
) : CartService {

    override fun getCartById(cartId: String): Cart {
        return try {
            apiRoot.carts()
                .withId(cartId)
                .get()
                .executeBlocking()
                .body;
        } catch (e: NotFoundException) {
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
        } catch (e: NotFoundException) {
            return ""
        }

        return if (prettyPrint) {
            val asObject: Any = objectMapper.readValue<Any>(body, Any::class.java)
            objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(asObject)

        } else {
            String(body)
        }
    }

    override fun createCart(): Cart {
        val cartDraft = CartDraftBuilder.of()
            .currency("GBP")
            .country("GB")
            .locale("en-GB")
            .shippingMode(ShippingMode.MULTIPLE)
            .taxRoundingMode(RoundingMode.HALF_DOWN)
            .build();

        val cart = apiRoot.carts()
            .post(cartDraft)
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
    override fun addItemToCart(
        cartId: String,
        cartVersion: Long,
        productId: String,
        variant: Long,
        quantity: Long
    ): Cart {
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

    fun addItemToCart(
        cartId: String, cartVersion: Long, productId: String, variant: Long, quantity: Long,
        fulfilmentOption: FulfilmentOption = FulfilmentOption.DELIVERY,
        address: Address? = null
    ): Cart {

        val addressToUse = address ?: dataStore.addresses()[0]
        println("Address to use: ${address}")

        var originalCart = getCartById(cartId)!!

        val actionsToPerform: MutableList<CartUpdateAction> = ArrayList()

        println("Creating adding a new line for the product, variant and quantity")
        val whatIsThis: CartAddLineItemAction = CartUpdateActionBuilder.of()
            .addLineItemBuilder()
            .productId(productId)
            .variantId(variant)
            .quantity(quantity)
            .build()

        actionsToPerform.add(whatIsThis)

        println("Adding a new address for the fulfilment option address")
        val addressKey = when (fulfilmentOption) {
            FulfilmentOption.DELIVERY, FulfilmentOption.DSV -> "USER_DELIVERY"
            else -> "STORE_ADDRESS"
        }
        println("Address key $addressKey")

        var shippingKey = fulfilmentOption.toString()

        // 1. At cart level add the address with the appropriate key - this will be referenced by the line item
        //       addShippingItemAddress - https://docs.commercetools.com/api/projects/carts#add-itemshippingaddress
        //
        val itemShippingItemAddressActions = updateItemShoppingAddressIfRequired(originalCart,
            addressKey,
            addressToUse)

        actionsToPerform.addAll(itemShippingItemAddressActions)
        println("Created a item shipping address with key ${addressKey}")


        // 2. At the cart level add the shipping method with the appropriate key
        //        addShippingMwthod - https://docs.commercetools.com/api/projects/carts#add-shippingmethod

        val shippingMethodIdRef: ShippingMethodResourceIdentifier = ShippingMethodResourceIdentifierBuilder.of()
            .id(dataStore.shippingMethods()[0].id) // NextDay - represents HOME DELIVERY HERE
            .build()

        val shippingMethodUpdateActions = updateCartShippingMethodIfRequired(
            originalCart,
            shippingKey,
            addressKey,
            addressToUse,
            shippingMethodIdRef
        )

        actionsToPerform.addAll(shippingMethodUpdateActions)

        // Run these actions to add the shipping line and add shipping information at top level of cart.
        val cartLevelUpdate = CartUpdateBuilder.of()
            .version(cartVersion)
            .actions(actionsToPerform)
            .build()
        var updatedCart = performUpdateCart(cartId, cartLevelUpdate)
        var newCartVersion = updatedCart.version

//        return updatedCart

        val itemFromCart = getItemFromCart(cartId, productId, variant)


        if (itemFromCart == null) {
            // ended early 11 Problem
            return updatedCart
        }

        val originalShippingTarget = itemFromCart.shippingDetails?.targets?.find { it.shippingMethodKey == fulfilmentOption.toString() }
        val newQuantity = if( originalShippingTarget != null ) {
            originalShippingTarget.quantity + quantity
        } else {
            quantity
        }

        //
        // 3. At the line item level add the shipping details - which must include the shipping method, the address and the quantity

        // one per fulfilment option
        val shippingTargetHome = ItemShippingTargetBuilder.of()
            .shippingMethodKey( shippingKey )
            .quantity(newQuantity)
            .addressKey(addressKey)
            .build()

        // add or plus a target for each fulfilment option
        val shippingDetailsDraft = ItemShippingDetailsDraftBuilder.of()
            .targets(shippingTargetHome)
            .build()

        val setLineItemAction = CartSetLineItemShippingDetailsActionBuilder.of()
            .lineItemId(itemFromCart.id)
            .shippingDetails(shippingDetailsDraft)
            .build()

        val cartUpdate = CartUpdateBuilder.of()
            .version(newCartVersion)
            .plusActions(setLineItemAction)
            .build()

        return performUpdateCart(cartId, cartUpdate)
    }

    /**
     * There's no update for existing Item Shipping Addresses, therefore have to delete and add again if need to change - say address.
     * Would this have any consequences if a line item was already referencing it? Depends on when the line item validation is performed.
     * This determines if the existing item shipping address needs to be deleted if the address has changed.
     */
    private fun updateItemShoppingAddressIfRequired(
        cart: Cart,
        key: String,
        address: Address
    ): MutableList<CartUpdateAction> {
        val potentialAddress = cart.itemShippingAddresses.find { it.key == key }
        val actions: MutableList<CartUpdateAction> = ArrayList()
        if (potentialAddress != null) {
            val identical = with(potentialAddress) {
                !equalsOrBothNull(company, address.company) ||
                        !equalsOrBothNull(firstName, address.firstName) ||
                        !equalsOrBothNull(lastName, address.lastName) ||
                        !equalsOrBothNull(streetNumber, address.streetNumber) ||
                        !equalsOrBothNull(streetName, address.streetName) ||
                        !equalsOrBothNull(city, address.city) ||
                        !equalsOrBothNull(postalCode, address.postalCode) ||
                        !equalsOrBothNull(country, address.country)
            }
            if (identical)
                println("Identical!!")
            return ArrayList()

            // different, so need to delete
            val cartRemoveItemShippingAddressAction: CartRemoveItemShippingAddressAction =
                CartRemoveItemShippingAddressActionBuilder.of()
                    .addressKey(key)
                    .build()

            actions.add(cartRemoveItemShippingAddressAction)
        }
        val cartAddItemShippingAddressAction: CartAddItemShippingAddressAction =
            CartAddItemShippingAddressActionBuilder.of()
                .address(createAddressBuilder(address, key).build())
                .build()

        actions.add(cartAddItemShippingAddressAction)

        return actions
    }

    /**
     * As with Item Shipping Addresses there is no update action - therefore have to remove and then add for an update.
     * Samw questions apply - will any references on the line items affect the delete? Who knows?
     */
    private fun updateCartShippingMethodIfRequired(
        cart: Cart, shippingKey: String, addressKey: String,
        address: Address, shippingMethodIdRef: ShippingMethodResourceIdentifier
    ): MutableList<CartUpdateAction> {

        val potentialMethod: Shipping? = cart.shipping.find { shipping -> shipping.shippingKey == shippingKey }
        val actions: MutableList<CartUpdateAction> = ArrayList()
        if (potentialMethod != null) {
            val identical = with(potentialMethod.shippingAddress) {
                !equalsOrBothNull(company, address.company) ||
                        !equalsOrBothNull(firstName, address.firstName) ||
                        !equalsOrBothNull(lastName, address.lastName) ||
                        !equalsOrBothNull(streetNumber, address.streetNumber) ||
                        !equalsOrBothNull(streetName, address.streetName) ||
                        !equalsOrBothNull(city, address.city) ||
                        !equalsOrBothNull(postalCode, address.postalCode) ||
                        !equalsOrBothNull(country, address.country)
            }
            if (identical)
                println("Identical shipping method!!")
            return ArrayList()

            // different, so need to delete original shipping method
            val cartRemoveShippingMethodAction = CartRemoveShippingMethodActionBuilder.of()
                .shippingKey(shippingKey)
                .build()

            actions.add(cartRemoveShippingMethodAction)
        }

        val addShippingMethodAction = CartAddShippingMethodActionBuilder.of()
            .shippingKey(shippingKey)  // unique key for this shipping method at cart level
            .shippingAddress(
                createAddressBuilder(
                    address,
                    addressKey
                ).build()
            ) // build a BaseAddress from local Address
            .shippingMethod(shippingMethodIdRef)
            .build()

        actions.add(addShippingMethodAction)

        return actions
    }

    fun createAddressBuilder(address: Address, key: String? = null): AddressBuilder {
        return AddressBuilder.of()
            .key(key ?: address.key)
            .firstName(address.firstName)
            .lastName(address.lastName)
            .company(address.company)
            .streetNumber(address.streetNumber)
            .streetName(address.streetName)
            .city(address.city)
            .postalCode(address.postalCode)
            .country(address.country)
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
    override fun removeItemFromCart(
        cartId: String,
        cartVersion: Long,
        productId: String,
        variant: Long,
        quantity: Long
    ): Cart {
        val itemFromCart: LineItem? = getItemFromCart(cartId, productId, variant)
        if (itemFromCart != null) {
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

    fun resetCartBeforePromotions(cart: Cart, cartVersion: Long): Cart {
        return cart
    }

    override fun deleteCart(cartId: String, cartVersion: Long): Cart {
        val cart = apiRoot.carts()
            .withId(cartId)
            .delete(cartVersion)
            .executeBlocking()
            .body;

        return cart
    }

    override fun addShippingAddress(cartId: String, cartVersion: Long, address: Address): Cart {
        return addShippingAddress(
            cartId, cartVersion,
            address.key,
            address.firstName,
            address.lastName,
            address.company,
            address.streetNumber,
            address.streetName,
            address.city,
            address.postalCode,
            address.country,
        )
    }

    override fun addShippingAddressWithKey(cartId: String, cartVersion: Long, key: String, address: Address): Cart {
        return addShippingAddress(
            cartId, cartVersion,
            key,
            address.firstName,
            address.lastName,
            address.company,
            address.streetNumber,
            address.streetName,
            address.city,
            address.postalCode,
            address.country,
        )
    }

    override fun addShippingAddress(
        cartId: String, cartVersion: Long,
        key: String, firstName: String?, lastName: String?, company: String?,
        streetNumber: String?, streetName: String?, city: String?, postalCode: String?, country: String?
    ): Cart {
        val draft = AddressDraftBuilder.of()
            .key(key)
            .firstName(firstName)
            .lastName(lastName)
            .company(company)
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
        println("Line items: ${cart.lineItems.size}, total: ${cart.totalLineItemQuantity}")
        println("Total price: ${printPrice(cart.totalPrice)}")
        println("Tax price: ${printPrice(cart.taxedPrice)}")
        println("Tax shipping price: ${printPrice(cart.taxedShippingPrice)}")
        println("Shipping mode: ${cart.shippingMode}")
        println("Shipping address (single): ${cart.shippingAddress}")
        println("Shipping: ")
        cart.shipping.forEach {
            println("\tKey: ${it.shippingKey}, associated address: ${printAddress(it.shippingAddress)} ")
            println("\t\t -  ${printShippingInfo(it.shippingInfo)} ")
        }

        println("Item Shipping Addresses:")
        cart.itemShippingAddresses.forEach {
            println("\tItem shipping address: ${printAddress(it)}")
        }

        var line = 1
        for (lineItem in lineItems) {
            val product = productService.getProductById(lineItem.productId)
            if (product != null) {
                println("${line}) Product: ${product.id} ${printLocalized(product.masterData.current.name)} - ${product.key},")
                println("\t quantity: ${lineItem.quantity}, totalPrice: ${printPrice(lineItem.totalPrice)}, taxedPrice: ${printPrice(lineItem.taxedPrice)}  ");
                println("\t\t ${ printItemShipping(lineItem.shippingDetails) }")
                println("\t\t  taxedPricePortions: ${ printMethodTaxedPrice(lineItem.taxedPricePortions) }")
//                println("\t\t ${ printMethodTaxRate(lineItem.perMethodTaxRate) }")
            }
            line++
        }
    }

    private fun performUpdateCart(cartId: String, cartUpdate: CartUpdate): Cart {
        return apiRoot.carts()
            .withId(cartId)
            .post(cartUpdate)
            .executeBlocking(Duration.ofMillis(2_000))
            .body;
    }

}