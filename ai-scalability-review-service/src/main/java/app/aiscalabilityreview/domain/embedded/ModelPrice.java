package app.aiscalabilityreview.domain.embedded;

import core.framework.mongo.Field;

/**
 * Price of one model, per million tokens.
 *
 * <p>Cached input is priced separately because ignoring it is wrong by a multiple rather than by a
 * rounding error: in the pilot run 3,155,521 of the validation stage's 3,931,963 input tokens came
 * from cache.
 */
public class ModelPrice {
    @Field(name = "model")
    public String model;

    @Field(name = "input_usd_per_million")
    public Double inputUsdPerMillion;

    @Field(name = "cached_input_usd_per_million")
    public Double cachedInputUsdPerMillion;

    @Field(name = "output_usd_per_million")
    public Double outputUsdPerMillion;
}
