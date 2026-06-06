package com.flipiq.deals.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DealEvaluationRequest(
		String propertyAddress,
		@NotNull @DecimalMin(value = "0.00") BigDecimal purchasePrice,
		@JsonAlias("arv")
		@NotNull @DecimalMin(value = "0.01") BigDecimal afterRepairValue,
		@JsonAlias({ "repairCost", "totalRepairCost" })
		@DecimalMin(value = "0.00") BigDecimal rehabCosts,
		@DecimalMin(value = "0.00") BigDecimal financingCosts,
		@DecimalMin(value = "0.00") BigDecimal holdingCosts,
		@DecimalMin(value = "0.00") BigDecimal sellingCosts,
		@DecimalMin(value = "0.00") BigDecimal profitBuffer,
		@DecimalMin(value = "0.01") BigDecimal maoRulePercentage,
		@DecimalMin(value = "0.00") BigDecimal closingCosts,
		Integer holdingMonths,
		String marketRisk,
		@Valid List<RehabItem> rehabItems,
		@Valid FinancingInput financing,
		@Valid RentalInput rentalAnalysis,
		@DecimalMin(value = "0.00") BigDecimal subjectPropertySquareFeet,
		@Valid List<ComparableSaleInput> comparableSales,
		@Valid FloridaCosts floridaCosts) {

	public DealEvaluationRequest(
			String propertyAddress,
			BigDecimal purchasePrice,
			BigDecimal afterRepairValue,
			BigDecimal rehabCosts,
			BigDecimal financingCosts,
			BigDecimal holdingCosts,
			BigDecimal sellingCosts,
			BigDecimal profitBuffer) {
		this(propertyAddress, purchasePrice, afterRepairValue, rehabCosts, financingCosts, holdingCosts, sellingCosts,
				profitBuffer, null, null, null, null, null, null, null, null, null, null);
	}

	public record RehabItem(
			String category,
			@NotNull @DecimalMin(value = "0.00") BigDecimal estimatedCost) {
	}

	public record FinancingInput(
			String financingType,
			@DecimalMin(value = "0.00") BigDecimal downPaymentPercentage,
			@DecimalMin(value = "0.00") BigDecimal interestRate,
			Integer loanTermMonths,
			@DecimalMin(value = "0.00") BigDecimal points) {
	}

	public record RentalInput(
			@DecimalMin(value = "0.00") BigDecimal monthlyRent,
			@DecimalMin(value = "0.00") BigDecimal monthlyMortgage,
			@DecimalMin(value = "0.00") BigDecimal propertyTax,
			@DecimalMin(value = "0.00") BigDecimal insurance,
			@DecimalMin(value = "0.00") BigDecimal hoa,
			@DecimalMin(value = "0.00") BigDecimal maintenance,
			@DecimalMin(value = "0.00") BigDecimal vacancyRate,
			@DecimalMin(value = "0.00") BigDecimal propertyManagementFee,
			@DecimalMin(value = "0.00") BigDecimal cashInvested) {
	}

	public record ComparableSaleInput(
			String address,
			@NotNull @DecimalMin(value = "0.00") BigDecimal salePrice,
			@NotNull @DecimalMin(value = "0.01") BigDecimal squareFeet,
			@DecimalMin(value = "0.00") BigDecimal distanceMiles,
			LocalDate soldDate,
			Integer bedrooms,
			BigDecimal bathrooms) {
	}

	public record FloridaCosts(
			@DecimalMin(value = "0.00") BigDecimal propertyTaxes,
			@DecimalMin(value = "0.00") BigDecimal insurance,
			@DecimalMin(value = "0.00") BigDecimal hoa,
			@DecimalMin(value = "0.00") BigDecimal realtorCommissionPercentage,
			@DecimalMin(value = "0.00") BigDecimal titleFees,
			@DecimalMin(value = "0.00") BigDecimal transferTaxes,
			@DecimalMin(value = "0.00") BigDecimal permitCosts,
			@DecimalMin(value = "0.00") BigDecimal floodInsurance) {
	}
}
