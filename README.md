# Carting demo

This is developed from playing around with CommerceTools and it's HTTP API (using CTs Java SDK).

And it's written in Kotlin! 😉

## Creating a Cart

On creating a new cart:
+ the total price is 0
+ the taxed price is null
+ the taxed shipping price is null
+ there are no line items
+ the status is active

```kotlin
// Create a cart
var cart = cartService.createCart()
```

## Adding an item to a cart (simple)

Add an item to the cart, including the product id, variant and quantity.
There is no shipping method here, so it's simple.

If a line item doesn’t exist for product and variant, then:

+ new line item created
+ quantity is set to given quantity
+ total price is price of product multiplied by quantity
+ line item total price is calculated for the item
+ line item taxed price is calculated - if a shipping address has been set

If line item already exists then

- new quantity is added to existing quantity
- prices are updated to reflect new quantity

At cart level:
+ total price is set accordingly
+ taxed prices is still null because no shipping details - these are only set when a shipping address is added
+ the taxed shipping price is null

```kotlin
// Add item to cart
cart = cartService.addItemToCart( CART_ID, cartVersion, PRODUCT_1.id, PRODUCT_1.variant, 2 );
```

## Removing an item from a cart (simple)

Removing an item is the opposite to adding, it reduces the line item quantity by the given amount and if zero then removes the line item.

- line item is reduced by quantity
- total price is reduced accordingly
- line item total price is calculated for the item

Cart level fields also updated accordingly.

```kotlin
cart = cartTrial.removeItemFromCart( CART_ID, cartVersion, PRODUCT_1.id, PRODUCT_1.variant, 1 )
```

## Adding an item to a cart (complex)

This involves adding a line item to the cart with a shipping method.
This allows quantity of a product at item line level to be associated with different shipping methods.
At the moment this is the mechanism for handling **Fulfilment Options** within CT.

Unlike the simple add and remove this is a bit more complex, because data needs to set at cart level before it can be referenced by line items. 
Therefore the item shipping address for the line and the shipping method information needs to be set before adding shipping details to the line.

+ new line item created
+ quantity is set to given quantity
+ total price is price of product multiplied by quantity
+ line item total price is calculated for the item
+ line item taxed price is calculated as there is now a shipping address(via the shipping method on line item) 
+ item shipping details contains a target for the shipping method, the quantity for that method and the address key

At cart level:
+ total price is set accordingly - it includes the tax and any shipping charges
+ taxed price is calculated, using the shipping address for the item
+ the taxed shipping price is calculated using the shipping method associated with the line item
+ the shipping contains an entry for the shipping method with a suitable key - this includes the Shipping Method id, name, associated address and shipping price

```kotlin
// Add item to cart with a fulfilment option
cart = cartService.addItemToCart( CART_ID, cartVersion, PRODUCT_2.id, PRODUCT_2.variant, 1, FulfilmentOption.DELIVERY, dataStore.addresses()[0] )
```

### Thoughts 🤔

+ Do we need to insist whenever a cart item is added the fulfilment option is included?
+ In order to add shipping method information to a line item you need to have added the shipping method information to the cart. The shipping method requires an address.
+ Also you need to add an item shipping address to cart level before it can be referenced by a line item.
+ There's no guarantee when we starting adding items to cart that we will have a delivery address (for Delivery), if it's for a store (CTC, TCND etc) the could possible have that.
+ Setting the targets on the line item appears a bit clunky, may need to be careful how this is handled. ALso does CT validate then total of target quantities equals the line item quantity?


# CommerceTools projects

The above code works against the **SF Sandbox** project - `sf-sandbox`.
This appears to be less controlled than SF Dev!

There are a number of **products** with a tax category. Need products with tax category so that tax can be calculated.

| Product Id                           | Name                                                                               | Price  | 
|--------------------------------------|------------------------------------------------------------------------------------|--------|
| d44ec4a7-f0b2-4f80-9bc6-df70b8d9500d | Erbauer EJS12-Li 12V Li-Ion EXT Brushless Cordless Jigsaw                          | £64.99 | 
| daf306c1-e113-48f7-ae61-e7d383530d39 | Hard Yakka Raptor Active Trousers Desert 38" W 32" L                               | £52.99 |
| 4ddb67ac-8f08-4c16-8804-8b8775e84e14 | Smith & Locke Stainless Brass BS 5-Lever Mortice Sashlock 66mm Case - 45mm Backset | £17.99 | 
| 5033ea4c-7c17-4e80-aeef-6f90895db97d | Schneider Electric Ultimate Slimline Slave Telephone Socket White                  | £3.96  | 

I've used an existing **Shipping Method** for testing.
This has a flat charge of £5 regardless of total cost etc.

| Id  | Name    |
|-----|---------|
|  4e744f6e-b4d7-4e6b-8f2c-103332af53a4   | NextDay |
