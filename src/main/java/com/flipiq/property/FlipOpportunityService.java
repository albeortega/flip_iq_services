package com.flipiq.property;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.flipiq.property.dto.FlipOpportunityPropertyDto;
import com.flipiq.property.dto.FlipOpportunityResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FlipOpportunityService {

	private static final Logger log = LoggerFactory.getLogger(FlipOpportunityService.class);

	private final RentCastListingsClient listingsClient;
	private final FlipScoreCalculator flipScoreCalculator;

	public FlipOpportunityService(RentCastListingsClient listingsClient, FlipScoreCalculator flipScoreCalculator) {
		this.listingsClient = listingsClient;
		this.flipScoreCalculator = flipScoreCalculator;
	}

	public FlipOpportunityResponse search(
			String zipCode,
			FlipOpportunitySort sort) {
		String cleanZipCode = requireZipCode(zipCode);
		FlipOpportunitySort cleanSort = sort == null ? FlipOpportunitySort.BEST_FLIP_SCORE : sort;

		log.info(
				"Flip opportunities search started: zipCode={}, sort={}",
				cleanZipCode,
				cleanSort);

		List<Map<String, Object>> listings = listingsClient.getActiveSaleListings(cleanZipCode);
		List<FlipOpportunityPropertyDto> calculatedProperties = new ArrayList<>();
		int missingRequiredDataCount = 0;

		for (Map<String, Object> listing : listings) {
			FlipOpportunityPropertyDto property = toOpportunity(listing, cleanZipCode);
			if (property == null) {
				missingRequiredDataCount++;
				continue;
			}

			log.info(
					"Flip opportunity calculated: zipCode={}, address='{}', listPrice={}, estimatedValue={}, estimatedRehabCost={}, closingCosts={}, holdingCosts={}, estimatedProfit={}, roiPercent={}, discountPercent={}, priceDropAmount={}, daysOnMarket={}, rehabRisk={}, flipScore={}, recommendation='{}'",
					cleanZipCode,
					property.address(),
					property.listPrice(),
					property.estimatedValue(),
					property.estimatedRehabCost(),
					property.closingCosts(),
					property.holdingCosts(),
					property.estimatedProfit(),
					property.roiPercent(),
					property.discountPercent(),
					property.priceDropAmount(),
					property.daysOnMarket(),
					property.rehabRisk(),
					property.flipScore(),
					property.recommendation());

			calculatedProperties.add(property);
		}

		List<FlipOpportunityPropertyDto> properties = calculatedProperties.stream()
				.sorted(comparator(cleanSort))
				.toList();

		log.info(
				"Flip opportunities search completed: zipCode={}, rawListings={}, returnedCount={}, missingRequiredDataCount={}",
				cleanZipCode,
				listings.size(),
				properties.size(),
				missingRequiredDataCount);
		return new FlipOpportunityResponse(
				cleanZipCode,
				properties.size(),
				cleanSort.name(),
				properties);
	}

	private FlipOpportunityPropertyDto toOpportunity(Map<String, Object> listing, String requestedZipCode) {
		BigDecimal listPrice = firstDecimal(
				listing.get("listPrice"),
				listing.get("price"),
				listing.get("salePrice"));
		BigDecimal estimatedValue = firstDecimal(
				listing.get("estimatedValue"),
				listing.get("value"),
				listing.get("priceEstimate"),
				listing.get("avmValue"));
		Integer livingArea = firstInteger(
				listing.get("livingArea"),
				listing.get("squareFootage"),
				listing.get("buildingArea"));

		if (listPrice == null || estimatedValue == null || livingArea == null || livingArea <= 0) {
			log.info(
					"Skipping listing with insufficient flip data: address='{}', listPrice={}, estimatedValue={}, livingArea={}, keys={}",
					address(listing),
					listPrice,
					estimatedValue,
					livingArea,
					listing.keySet());
			return null;
		}

		Integer yearBuilt = firstInteger(listing.get("yearBuilt"));
		String rehabRisk = rehabRisk(yearBuilt);
		BigDecimal estimatedRehabCost = rehabCostPerSquareFoot(rehabRisk)
				.multiply(BigDecimal.valueOf(livingArea))
				.setScale(0, RoundingMode.HALF_UP);
		BigDecimal closingCosts = listPrice.multiply(BigDecimal.valueOf(0.04)).setScale(0, RoundingMode.HALF_UP);
		BigDecimal holdingCosts = listPrice.multiply(BigDecimal.valueOf(0.025)).setScale(0, RoundingMode.HALF_UP);
		BigDecimal estimatedProfit = estimatedValue
				.subtract(listPrice)
				.subtract(estimatedRehabCost)
				.subtract(closingCosts)
				.subtract(holdingCosts)
				.setScale(0, RoundingMode.HALF_UP);
		BigDecimal totalCashNeeded = listPrice.add(estimatedRehabCost).add(closingCosts).add(holdingCosts);
		BigDecimal roiPercent = totalCashNeeded.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: estimatedProfit.divide(totalCashNeeded, 6, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.setScale(1, RoundingMode.HALF_UP);
		BigDecimal discountPercent = estimatedValue.compareTo(BigDecimal.ZERO) == 0
				? BigDecimal.ZERO
				: estimatedValue.subtract(listPrice)
						.divide(estimatedValue, 6, RoundingMode.HALF_UP)
						.multiply(BigDecimal.valueOf(100))
						.setScale(1, RoundingMode.HALF_UP);
		BigDecimal priceDropAmount = priceDropAmount(listing, listPrice);
		Integer daysOnMarket = daysOnMarket(listing);
		int flipScore = flipScoreCalculator.calculate(
				estimatedProfit,
				roiPercent,
				discountPercent,
				daysOnMarket,
				priceDropAmount,
				rehabRisk);

		return new FlipOpportunityPropertyDto(
				firstText(asString(listing.get("id")), asString(listing.get("listingId")), generatedId(listing)),
				address(listing),
				firstText(asString(listing.get("city")), asString(listing.get("addressCity"))),
				firstText(asString(listing.get("state")), asString(listing.get("stateCode"))),
				firstText(asString(listing.get("zipCode")), requestedZipCode),
				firstText(asString(listing.get("propertyType")), asString(listing.get("propertySubType"))),
				listPrice,
				estimatedValue,
				estimatedRehabCost,
				closingCosts,
				holdingCosts,
				estimatedProfit,
				roiPercent,
				discountPercent,
				priceDropAmount.setScale(0, RoundingMode.HALF_UP),
				daysOnMarket,
				rehabRisk,
				firstInteger(listing.get("bedrooms"), listing.get("beds")),
				firstDecimal(listing.get("bathrooms"), listing.get("baths")),
				livingArea,
				yearBuilt,
				firstDouble(listing.get("latitude")),
				firstDouble(listing.get("longitude")),
				flipScore,
				flipScoreCalculator.recommendation(flipScore),
				highlights(estimatedProfit, discountPercent, priceDropAmount));
	}

	private Comparator<FlipOpportunityPropertyDto> comparator(FlipOpportunitySort sort) {
		Comparator<FlipOpportunityPropertyDto> secondary = Comparator
				.comparing(FlipOpportunityPropertyDto::estimatedProfit, Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparing(FlipOpportunityPropertyDto::roiPercent, Comparator.nullsLast(Comparator.reverseOrder()));
		return switch (sort) {
			case HIGHEST_PROFIT -> Comparator
					.comparing(FlipOpportunityPropertyDto::estimatedProfit, Comparator.nullsLast(Comparator.reverseOrder()))
					.thenComparing(secondary);
			case HIGHEST_ROI -> Comparator
					.comparing(FlipOpportunityPropertyDto::roiPercent, Comparator.nullsLast(Comparator.reverseOrder()))
					.thenComparing(secondary);
			case BIGGEST_DISCOUNT -> Comparator
					.comparing(FlipOpportunityPropertyDto::discountPercent, Comparator.nullsLast(Comparator.reverseOrder()))
					.thenComparing(secondary);
			case LOWEST_LIST_PRICE -> Comparator
					.comparing(FlipOpportunityPropertyDto::listPrice, Comparator.nullsLast(Comparator.naturalOrder()))
					.thenComparing(secondary);
			case NEWEST_LISTING -> Comparator
					.comparing(FlipOpportunityPropertyDto::daysOnMarket, Comparator.nullsLast(Comparator.naturalOrder()))
					.thenComparing(secondary);
			case BIGGEST_PRICE_DROP -> Comparator
					.comparing(FlipOpportunityPropertyDto::priceDropAmount, Comparator.nullsLast(Comparator.reverseOrder()))
					.thenComparing(secondary);
			case BEST_FLIP_SCORE -> Comparator
					.comparing(FlipOpportunityPropertyDto::flipScore, Comparator.nullsLast(Comparator.reverseOrder()))
					.thenComparing(secondary);
		};
	}

	private String requireZipCode(String zipCode) {
		if (zipCode == null || !zipCode.matches("\\d{5}")) {
			log.warn("Flip opportunities validation failed: zipCode must be exactly 5 digits");
			throw new IllegalArgumentException("Please enter a valid 5-digit ZIP code.");
		}
		return zipCode;
	}

	private String rehabRisk(Integer yearBuilt) {
		if (yearBuilt == null) {
			return "Medium";
		}
		if (yearBuilt >= 2005) {
			return "Light";
		}
		if (yearBuilt >= 1980) {
			return "Medium";
		}
		return "Heavy";
	}

	private BigDecimal rehabCostPerSquareFoot(String rehabRisk) {
		return switch (rehabRisk) {
			case "Light" -> BigDecimal.valueOf(25);
			case "Heavy" -> BigDecimal.valueOf(75);
			default -> BigDecimal.valueOf(45);
		};
	}

	private BigDecimal priceDropAmount(Map<String, Object> listing, BigDecimal listPrice) {
		BigDecimal directPriceDrop = firstDecimal(
				listing.get("priceDropAmount"),
				listing.get("priceChange"),
				listing.get("lastPriceChangeAmount"));
		if (directPriceDrop != null) {
			return directPriceDrop.abs();
		}

		BigDecimal originalPrice = firstDecimal(
				listing.get("originalPrice"),
				listing.get("initialListPrice"),
				listing.get("previousPrice"));
		if (originalPrice == null) {
			return BigDecimal.ZERO;
		}
		return originalPrice.subtract(listPrice).max(BigDecimal.ZERO);
	}

	private Integer daysOnMarket(Map<String, Object> listing) {
		Integer directDays = firstInteger(listing.get("daysOnMarket"), listing.get("dom"));
		if (directDays != null) {
			return directDays;
		}

		LocalDate listedDate = firstDate(
				asString(listing.get("listedDate")),
				asString(listing.get("createdDate")),
				asString(listing.get("dateListed")));
		if (listedDate == null) {
			return null;
		}
		return Math.max(0, (int) ChronoUnit.DAYS.between(listedDate, LocalDate.now()));
	}

	private String address(Map<String, Object> listing) {
		String formattedAddress = firstText(
				asString(listing.get("formattedAddress")),
				asString(listing.get("address")),
				asString(listing.get("addressLine1")));
		if (formattedAddress != null) {
			return formattedAddress;
		}

		List<String> parts = new ArrayList<>();
		addIfPresent(parts, asString(listing.get("streetAddress")));
		addIfPresent(parts, asString(listing.get("city")));
		addIfPresent(parts, asString(listing.get("state")));
		addIfPresent(parts, asString(listing.get("zipCode")));
		return parts.isEmpty() ? "Address unavailable" : String.join(", ", parts);
	}

	private String generatedId(Map<String, Object> listing) {
		return UUID.nameUUIDFromBytes(address(listing).getBytes()).toString();
	}

	private List<String> highlights(BigDecimal estimatedProfit, BigDecimal discountPercent, BigDecimal priceDropAmount) {
		List<String> highlights = new ArrayList<>();
		highlights.add("Estimated $%,.0f profit".formatted(estimatedProfit));
		highlights.add("%s%% below estimated value".formatted(discountPercent.stripTrailingZeros().toPlainString()));
		if (priceDropAmount.compareTo(BigDecimal.ZERO) > 0) {
			highlights.add("Price dropped $%,.0f".formatted(priceDropAmount));
		}
		return highlights;
	}

	private void addIfPresent(List<String> parts, String value) {
		if (value != null && !value.isBlank()) {
			parts.add(value);
		}
	}

	private String firstText(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private BigDecimal firstDecimal(Object... values) {
		for (Object value : values) {
			BigDecimal decimal = asBigDecimal(value);
			if (decimal != null) {
				return decimal;
			}
		}
		return null;
	}

	private Integer firstInteger(Object... values) {
		for (Object value : values) {
			if (value instanceof Number number) {
				return number.intValue();
			}
			if (value instanceof String string && !string.isBlank()) {
				try {
					return new BigDecimal(string).intValue();
				} catch (NumberFormatException ignored) {
					// Try the next candidate.
				}
			}
		}
		return null;
	}

	private Double firstDouble(Object... values) {
		for (Object value : values) {
			if (value instanceof Number number) {
				return number.doubleValue();
			}
			if (value instanceof String string && !string.isBlank()) {
				try {
					return Double.valueOf(string);
				} catch (NumberFormatException ignored) {
					// Try the next candidate.
				}
			}
		}
		return null;
	}

	private BigDecimal asBigDecimal(Object value) {
		if (value instanceof BigDecimal decimal) {
			return decimal;
		}
		if (value instanceof Number number) {
			return BigDecimal.valueOf(number.doubleValue());
		}
		if (value instanceof String string && !string.isBlank()) {
			try {
				return new BigDecimal(string);
			} catch (NumberFormatException ignored) {
				return null;
			}
		}
		return null;
	}

	private String asString(Object value) {
		return value instanceof String string ? string : null;
	}

	private LocalDate firstDate(String... values) {
		for (String value : values) {
			LocalDate date = parseDate(value);
			if (date != null) {
				return date;
			}
		}
		return null;
	}

	private LocalDate parseDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return OffsetDateTime.parse(value).toLocalDate();
		} catch (DateTimeParseException ignored) {
			// Try ISO local date below.
		}
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException ignored) {
			return null;
		}
	}
}
