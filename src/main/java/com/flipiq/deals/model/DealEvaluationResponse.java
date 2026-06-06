package com.flipiq.deals.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DealEvaluationResponse(
		String propertyAddress,
		BigDecimal purchasePrice,
		BigDecimal afterRepairValue,
		BigDecimal arv,
		BigDecimal offerRulePercentage,
		BigDecimal maoRulePercentage,
		BigDecimal ruleValue,
		BigDecimal rehabCosts,
		BigDecimal repairCost,
		BigDecimal totalRepairCost,
		BigDecimal financingCosts,
		BigDecimal holdingCosts,
		BigDecimal sellingCosts,
		BigDecimal closingCosts,
		BigDecimal profitBuffer,
		BigDecimal totalProjectCost,
		BigDecimal maximumOffer,
		BigDecimal maximumAllowableOffer,
		Boolean isOfferAcceptable,
		BigDecimal projectedProfit,
		BigDecimal roi,
		BigDecimal profitMargin,
		BigDecimal offerSpread,
		Integer dealScore,
		String scoreGrade,
		String riskLevel,
		List<String> riskReasons,
		String recommendation,
		FinancingBreakdown financing,
		RentalAnalysis rentalAnalysis,
		CompsAnalysis compsAnalysis,
		List<SensitivityScenario> scenarios) {

	public record FinancingBreakdown(
			String financingType,
			BigDecimal downPaymentPercentage,
			BigDecimal loanAmount,
			BigDecimal interestRate,
			Integer loanTermMonths,
			BigDecimal points,
			BigDecimal monthlyPayment,
			BigDecimal totalInterestCost,
			BigDecimal pointsCost,
			BigDecimal totalFinancingCost) {
	}

	public record RentalAnalysis(
			BigDecimal monthlyRent,
			BigDecimal monthlyMortgage,
			BigDecimal monthlyExpenses,
			BigDecimal monthlyCashFlow,
			BigDecimal annualCashFlow,
			BigDecimal capRate,
			BigDecimal cashOnCashReturn,
			BigDecimal dscr) {
	}

	public record CompsAnalysis(
			BigDecimal averagePricePerSqFt,
			BigDecimal estimatedArv,
			List<ComparableSale> comparableSales) {
	}

	public record ComparableSale(
			String address,
			BigDecimal salePrice,
			BigDecimal squareFeet,
			BigDecimal pricePerSqFt,
			BigDecimal distanceMiles,
			LocalDate soldDate,
			Integer bedrooms,
			BigDecimal bathrooms) {
	}

	public record SensitivityScenario(
			String name,
			BigDecimal profit,
			BigDecimal roi) {
	}
}
