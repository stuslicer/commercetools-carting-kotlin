package com.kingfisher.kotlinbasketdemo.utils

import com.commercetools.api.models.cart.TaxedItemPrice
import com.commercetools.api.models.cart.TaxedPrice
import com.commercetools.api.models.common.CentPrecisionMoney
import com.commercetools.api.models.common.LocalizedString
import com.commercetools.api.models.common.LocalizedStringEntry

class CartUtils {
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
