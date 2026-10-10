package app.aiscalabilityreview.api;

import app.aiscalabilityreview.api.preset.ListPresetRequest;
import app.aiscalabilityreview.api.preset.ListPresetResponse;
import app.aiscalabilityreview.api.preset.PresetActionResponse;
import app.aiscalabilityreview.api.preset.PresetView;
import app.aiscalabilityreview.api.preset.SavePresetRequest;
import app.aiscalabilityreview.api.settings.SettingsView;
import core.framework.api.http.HTTPStatus;
import core.framework.api.web.service.DELETE;
import core.framework.api.web.service.GET;
import core.framework.api.web.service.POST;
import core.framework.api.web.service.PUT;
import core.framework.api.web.service.Path;
import core.framework.api.web.service.PathParam;
import core.framework.api.web.service.ResponseStatus;

public interface SettingsWebService {
    @GET
    @Path("/settings")
    SettingsView get();

    @PUT
    @Path("/settings")
    SettingsView update(SettingsView request);

    @GET
    @Path("/preset")
    ListPresetResponse listPreset(ListPresetRequest request);

    @POST
    @ResponseStatus(HTTPStatus.CREATED)
    @Path("/preset")
    PresetView savePreset(SavePresetRequest request);

    @DELETE
    @Path("/preset/:presetId")
    PresetActionResponse deletePreset(@PathParam("presetId") String presetId);
}
