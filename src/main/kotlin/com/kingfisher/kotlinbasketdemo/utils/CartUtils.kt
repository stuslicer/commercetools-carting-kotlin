package com.kingfisher.kotlinbasketdemo.utils

import com.commercetools.api.models.cart.ItemShippingDetails
import com.commercetools.api.models.cart.ShippingInfo
import com.commercetools.api.models.cart.TaxedItemPrice
import com.commercetools.api.models.cart.TaxedPrice
import com.commercetools.api.models.common.Address
import com.commercetools.api.models.common.CentPrecisionMoney
import com.commercetools.api.models.common.LocalizedString
import com.commercetools.api.models.common.LocalizedStringEntry
import java.lang.StringBuilder

fun equalsOrBothNull(left: Any?, right: Any?) : Boolean {
    when {
        left != null && right != null && left == right -> true
        left == null && right == null -> true
        else -> false
    }
    return false
}

fun printPrice(money: CentPrecisionMoney): String? {
    return String.format(
        "%s %.2f", money.currency.currencyCode,
        money.centAmount / Math.pow(10.0, money.fractionDigits.toDouble())
    )
}

fun printPrice(taxedPrice: TaxedPrice?): String? {
    return if (taxedPrice == null) {
        ""
    } else String.format(
        "net: %s, tax: %s, total: %s",
        printPrice(taxedPrice.totalNet),
        printPrice(taxedPrice.totalTax),
        printPrice(taxedPrice.totalGross)
    )
}

fun printPrice(taxedPrice: TaxedItemPrice?): String? {
    return if (taxedPrice == null) {
        ""
    } else String.format(
        "net: %s, tax: %s, total: %s",
        printPrice(taxedPrice.totalNet),
        printPrice(taxedPrice.totalTax),
        printPrice(taxedPrice.totalGross)
    )
}

fun printLocalized(localizedString: LocalizedString?): String? {
    return localizedString?.stream()?.filter { u: LocalizedStringEntry? -> u != null }?.findFirst()
        ?.map { obj: LocalizedStringEntry -> obj.value }?.orElse(null)
}

fun printAddress( address: Address? ) : String?{
    if( address != null ) {
        var sb = StringBuilder().apply {
            append("key: ${address.key}, ")
            if (address.firstName != null) {
                append("name: ${address.firstName} ${address.lastName}, ")
            }
            if (address.company != null) {
                append("company: ${address.company}, ")
            }
            append("details: ${address.streetNumber} ${address.firstName} ${address.city} ${address.postalCode} country: ${address.country}")
        }
        return sb.toString()
    }
    return ""
}

fun printShippingInfo(shippingInfo: ShippingInfo): String {
    return "name (shipping method): ${shippingInfo.shippingMethodName} price: ${printPrice( shippingInfo.price )}, " +
            "taxedPrice: ${printPrice( shippingInfo.taxedPrice )} id (shipping method): ${shippingInfo.shippingMethod.id}"
}

fun printItemShipping(details: ItemShippingDetails?): String {
    if( details == null ) {
        return ""
    }
    return StringBuffer().apply {
        append(" targets: ")
        details.targets.forEach {
            append(" ${it.shippingMethodKey} - ${it.quantity} ${it.addressKey} ")
        }
    }.toString()
}