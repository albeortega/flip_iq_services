package com.flipiq.property;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class FlipScoreCalculator {

	public int calculate(
			BigDecimal estimatedProfit,
			BigDecimal roiPercent,
			BigDecimal discountPercent,
			Integer daysOnMarket,
			BigDecimal priceDropAmount,
			String rehabRisk) {
		double score =
				profitScore(estimatedProfit) * 0.35
						+ ratioScore(roiPercent, 25) * 0.25
						+ ratioScore(discountPercent, 25) * 0.15
						+ daysOnMarketScore(daysOnMarket) * 0.10
						+ ratioScore(priceDropAmount, 50000) * 0.10
						+ rehabRiskScore(rehabRisk) * 0.05;
		return Math.max(0, Math.min(100, (int) Math.round(score)));
	}

	public String recommendation(int flipScore) {
		if (flipScore >= 80) {
			return "Strong Flip Candidate";
		}
		if (flipScore >= 65) {
			return "Good Deal";
		}
		if (flipScore >= 50) {
			return "Maybe";
		}
		return "Avoid";
	}

	private double profitScore(BigDecimal estimatedProfit) {
		return ratioScore(estimatedProfit, 100000);
	}

	private double ratioScore(BigDecimal value, double max) {
		if (value == null || max <= 0) {
			return 0;
		}
		return value.divide(BigDecimal.valueOf(max), 6, RoundingMode.HALF_UP)
				.multiply(BigDecimal.valueOf(100))
				.min(BigDecimal.valueOf(100))
				.max(BigDecimal.ZERO)
				.doubleValue();
	}

	private double daysOnMarketScore(Integer daysOnMarket) {
		if (daysOnMarket == null || daysOnMarket <= 0) {
			return 0;
		}
		return Math.min((daysOnMarket / 90.0) * 100, 100);
	}

	private double rehabRiskScore(String rehabRisk) {
		return switch (rehabRisk == null ? "" : rehabRisk) {
			case "Light" -> 100;
			case "Heavy" -> 25;
			default -> 60;
		};
	}
}
