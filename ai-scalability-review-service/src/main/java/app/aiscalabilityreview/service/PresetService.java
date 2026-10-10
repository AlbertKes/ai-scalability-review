package app.aiscalabilityreview.service;

import app.aiscalabilityreview.api.preset.ListPresetRequest;
import app.aiscalabilityreview.api.preset.ListPresetResponse;
import app.aiscalabilityreview.api.preset.PresetActionResponse;
import app.aiscalabilityreview.api.preset.PresetView;
import app.aiscalabilityreview.api.preset.SavePresetRequest;
import app.aiscalabilityreview.domain.ReviewPreset;
import app.aiscalabilityreview.service.builder.LocalReviewRequestMapper;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import core.framework.inject.Inject;
import core.framework.mongo.MongoCollection;
import core.framework.mongo.Query;
import core.framework.util.Strings;
import core.framework.web.exception.NotFoundException;
import org.bson.conversions.Bson;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Saved trigger parameters per service and environment.
 *
 * <p>Stored as given, incomplete or not: the rules in {@link LocalReviewValidator} run when a
 * preset is used, not when it is written, so a half-filled form can still be parked.
 */
public class PresetService {
    @Inject
    MongoCollection<ReviewPreset> reviewPresetCollection;

    public ListPresetResponse list(ListPresetRequest request) {
        List<Bson> filters = new ArrayList<>();
        if (!Strings.isBlank(request.serviceId)) filters.add(Filters.eq("service_id", request.serviceId));
        if (!Strings.isBlank(request.env)) filters.add(Filters.eq("env", request.env));
        Query query = new Query();
        query.filter = filters.isEmpty() ? Filters.empty() : Filters.and(filters);
        query.sort = Sorts.ascending("service_id", "name");

        ListPresetResponse response = new ListPresetResponse();
        response.presets = reviewPresetCollection.find(query).stream().map(this::toView).toList();
        return response;
    }

    public PresetView save(SavePresetRequest request) {
        ReviewPreset preset = Strings.isBlank(request.presetId)
            ? newPreset() : reviewPresetCollection.get(request.presetId)
                .orElseThrow(() -> new NotFoundException("Preset not found: " + request.presetId));
        preset.serviceId = request.serviceId;
        preset.env = request.env;
        preset.name = request.name;
        preset.request = LocalReviewRequestMapper.toSnapshot(request.request);
        preset.isDefault = Boolean.TRUE.equals(request.isDefault);
        preset.createdBy = request.createdBy;
        preset.updatedAt = ZonedDateTime.now();

        if (Boolean.TRUE.equals(preset.isDefault)) clearDefault(preset.serviceId, preset.presetId);
        if (Strings.isBlank(request.presetId)) {
            reviewPresetCollection.insert(preset);
        } else {
            reviewPresetCollection.replace(preset);
        }
        return toView(preset);
    }

    public PresetActionResponse delete(String presetId) {
        reviewPresetCollection.delete(presetId);
        PresetActionResponse response = new PresetActionResponse();
        response.presetId = presetId;
        response.message = "Preset deleted.";
        return response;
    }

    private void clearDefault(String serviceId, String keepPresetId) {
        reviewPresetCollection.update(
            Filters.and(Filters.eq("service_id", serviceId), Filters.ne("_id", keepPresetId)),
            Updates.set("is_default", Boolean.FALSE));
    }

    private ReviewPreset newPreset() {
        ReviewPreset preset = new ReviewPreset();
        preset.presetId = UUID.randomUUID().toString();
        preset.createdAt = ZonedDateTime.now();
        return preset;
    }

    private PresetView toView(ReviewPreset preset) {
        PresetView view = new PresetView();
        view.presetId = preset.presetId;
        view.serviceId = preset.serviceId;
        view.env = preset.env;
        view.name = preset.name;
        view.request = LocalReviewRequestMapper.toView(preset.request);
        view.isDefault = preset.isDefault;
        view.createdBy = preset.createdBy;
        view.createdAt = preset.createdAt;
        view.updatedAt = preset.updatedAt;
        return view;
    }
}
