/*
 * Copyright (c) 2002-2022, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.genericattributes.modules.identitypicker.service.entrytype;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import fr.paris.lutece.plugins.genericattributes.business.Entry;
import fr.paris.lutece.plugins.genericattributes.business.Field;
import fr.paris.lutece.plugins.genericattributes.business.GenericAttributeError;
import fr.paris.lutece.plugins.genericattributes.business.MandatoryError;
import fr.paris.lutece.plugins.genericattributes.business.Response;
import fr.paris.lutece.plugins.genericattributes.service.entrytype.EntryTypeService;
import fr.paris.lutece.plugins.genericattributes.util.GenericAttributesUtils;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AttributeKeyDto;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.AuthorType;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.common.RequestAuthor;
import fr.paris.lutece.plugins.identitystore.v3.web.rs.dto.referentiel.AttributeSearchResponse;
import fr.paris.lutece.plugins.identitystore.v3.web.service.ReferentialService;
import fr.paris.lutece.plugins.identitystore.web.exception.IdentityStoreException;
import fr.paris.lutece.portal.business.regularexpression.RegularExpression;
import fr.paris.lutece.portal.service.i18n.I18nService;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.plugin.Plugin;
import fr.paris.lutece.portal.service.regularexpression.RegularExpressionService;
import fr.paris.lutece.portal.service.spring.SpringContextService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.ReferenceList;
import fr.paris.lutece.util.string.StringUtil;

/**
 * Abstract entry type for text
 */
public abstract class AbstractEntryTypeIdentityPicker extends EntryTypeService
{
	private static final String BEAN_REFERENTIAL_SERVICE = "identity.ReferentialService";
	
	private static final String PARAMETER_CUID = "field_customer_id";
	private static final String PARAMETER_IDENTITY_ATTRIBUTE = "identity_attribute";
	private static final String PARAMETER_CREATE_ATTRIBUTE = "create_identity";
	private static final String PARAMETER_MODIFY_ATTRIBUTE = "modify_identity";
	private static final String PARAMETER_STORAGE_ATTRIBUTE = "storage_identity";
	private static final String FIELD_LIST_ATTRIBUTES = "lst_attributes";
	private static final String FIELD_CREATE_IDENTITY = "create_identity";
	private static final String FIELD_MODIFY_IDENTITY = "modify_identity";
	private static final String FIELD_STORAGE_IDENTITY = "storage_identity";
	
	public static final String PROPERTY_CLIENT_CODE = "identitypicker.default.client.code";
	
	private ReferentialService _referentialService;
    /**
     * {@inheritDoc}
     */
    @Override
    public String getRequestData( Entry entry, HttpServletRequest request, Locale locale )
    {
        initCommonRequestData( entry, request );
        String strCode = request.getParameter( PARAMETER_ENTRY_CODE );
        String strTitle = request.getParameter( PARAMETER_TITLE );
        String strHelpMessage = ( request.getParameter( PARAMETER_HELP_MESSAGE ) != null ) ? request.getParameter( PARAMETER_HELP_MESSAGE ).trim( ) : null;
        String strComment = request.getParameter( PARAMETER_COMMENT );
        String strValue = request.getParameter( PARAMETER_VALUE );
        String strMandatory = request.getParameter( PARAMETER_MANDATORY );
        String strWidth = request.getParameter( PARAMETER_WIDTH );
        String strMaxSizeEnter = request.getParameter( PARAMETER_MAX_SIZE_ENTER );
        String strConfirmField = request.getParameter( PARAMETER_CONFIRM_FIELD );
        String strConfirmFieldTitle = request.getParameter( PARAMETER_CONFIRM_FIELD_TITLE );
        String strUnique = request.getParameter( PARAMETER_UNIQUE );
        String strCSSClass = request.getParameter( PARAMETER_CSS_CLASS );
        String strOnlyDisplayInBack = request.getParameter( PARAMETER_ONLY_DISPLAY_IN_BACK );
        String strErrorMessage = request.getParameter( PARAMETER_ERROR_MESSAGE );
        String strIndexed = request.getParameter( PARAMETER_INDEXED );
        String strPlaceholder = request.getParameter( PARAMETER_PLACEHOLDER );
        String[] strIdentityAttribute = request.getParameterValues (PARAMETER_IDENTITY_ATTRIBUTE );
        String strCreateIdentity = request.getParameter( PARAMETER_CREATE_ATTRIBUTE );
        String strModifyIdentity = request.getParameter( PARAMETER_MODIFY_ATTRIBUTE );
        String strStorageIdentity = request.getParameter( PARAMETER_STORAGE_ATTRIBUTE );

        int nWidth = -1;
        int nMaxSizeEnter = -1;

        String strFieldError = StringUtils.EMPTY;

        if ( StringUtils.isBlank( strTitle ) )
        {
            strFieldError = ERROR_FIELD_TITLE;
        }

        else
            if ( StringUtils.isBlank( strWidth ) )
            {
                strFieldError = ERROR_FIELD_WIDTH;
            }

        if ( ( strConfirmField != null ) && StringUtils.isBlank( strConfirmFieldTitle ) )
        {
            strFieldError = FIELD_CONFIRM_FIELD_TITLE;
        }

        if ( StringUtils.isNotBlank( strFieldError ) )
        {
            Object [ ] tabRequiredFields = {
                    I18nService.getLocalizedString( strFieldError, locale )
            };

            return AdminMessageService.getMessageUrl( request, MESSAGE_MANDATORY_FIELD, tabRequiredFields, AdminMessage.TYPE_STOP );
        }

        try
        {
            nWidth = Integer.parseInt( strWidth );
        }
        catch( NumberFormatException ne )
        {
            strFieldError = ERROR_FIELD_WIDTH;
        }

        try
        {
            if ( StringUtils.isNotBlank( strMaxSizeEnter ) )
            {
                nMaxSizeEnter = Integer.parseInt( strMaxSizeEnter );
            }
        }
        catch( NumberFormatException ne )
        {
            strFieldError = FIELD_MAX_SIZE_ENTER;
        }

        if ( StringUtils.isNotBlank( strFieldError ) )
        {
            Object [ ] tabRequiredFields = {
                    I18nService.getLocalizedString( strFieldError, locale )
            };

            return AdminMessageService.getMessageUrl( request, MESSAGE_NUMERIC_FIELD, tabRequiredFields, AdminMessage.TYPE_STOP );
        }

        entry.setTitle( strTitle );
        entry.setHelpMessage( strHelpMessage );
        entry.setComment( strComment );
        entry.setCSSClass( strCSSClass );
        entry.setIndexed( strIndexed != null );
        entry.setErrorMessage( strErrorMessage );
        entry.setCode( strCode );
        
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_TEXT_CONF, null, strValue );
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_WIDTH, null, String.valueOf( nWidth ) );
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_MAX_SIZE, null, String.valueOf( nMaxSizeEnter ) );
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_PLACEHOLDER, null, strPlaceholder != null ? strPlaceholder : StringUtils.EMPTY );
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_LIST_ATTRIBUTES, null, String.join(",", strIdentityAttribute) );
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_CREATE_IDENTITY, null, String.valueOf(strCreateIdentity) );
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_MODIFY_IDENTITY, null, String.valueOf(strModifyIdentity) );
        
        for (String strAttribute : strIdentityAttribute) {
        	GenericAttributesUtils.createOrUpdateField( entry, strAttribute, strAttribute, strAttribute );
        }
        
        boolean confirmStorage = false;
        if ( strStorageIdentity != null )
        {
        	confirmStorage = true;
        }
        
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_STORAGE_IDENTITY, null, String.valueOf(confirmStorage) );
        
        boolean confirmModityIdentity = false;
        if ( strModifyIdentity != null )
        {
        	confirmModityIdentity = true;
        }
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_MODIFY_IDENTITY, null, String.valueOf(confirmModityIdentity) );

        boolean confirmCreateIdentity = false;
        if ( strCreateIdentity != null )
        {
        	confirmCreateIdentity = true;
        }
        GenericAttributesUtils.createOrUpdateField( entry, FIELD_CREATE_IDENTITY, null, String.valueOf(confirmCreateIdentity) );
        
        entry.setMandatory( strMandatory != null );
        entry.setOnlyDisplayInBack( strOnlyDisplayInBack != null );

        boolean confirm = false;
        String fieldTitle = null;
        if ( strConfirmField != null )
        {
            confirm = true;
            fieldTitle = strConfirmFieldTitle;
        }

        GenericAttributesUtils.createOrUpdateField( entry, FIELD_CONFIRM, fieldTitle, String.valueOf( confirm ) );
        entry.setUnique( strUnique != null );
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReferenceList getReferenceListRegularExpression( Entry entry, Plugin plugin )
    {
        ReferenceList refListRegularExpression = null;

        if ( RegularExpressionService.getInstance( ).isAvailable( ) )
        {
            refListRegularExpression = new ReferenceList( );

            List<RegularExpression> listRegularExpression = RegularExpressionService.getInstance( ).getAllRegularExpression( );

            for ( RegularExpression regularExpression : listRegularExpression )
            {
                if ( !entry.getFieldByCode( FIELD_TEXT_CONF ).getRegularExpressionList( ).contains( regularExpression ) )
                {
                    refListRegularExpression.addItem( regularExpression.getIdExpression( ), regularExpression.getTitle( ) );
                }
            }
        }

        return refListRegularExpression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GenericAttributeError getResponseData( Entry entry, HttpServletRequest request, List<Response> listResponse, Locale locale )
    {
    	List<String> lstAttributes = new ArrayList<>( );
    	
    	String strValueCuid = StringUtils.trim( request.getParameter( PARAMETER_CUID ) );
    	String strAttributes = entry.getFieldByCode( FIELD_LIST_ATTRIBUTES ).getValue( );
    	lstAttributes = Arrays.asList(strAttributes.split(","));
    	
    	if ( strValueCuid == null )
        {
            return null;
        }
        
        Field confirmField = entry.getFieldByCode( FIELD_CONFIRM );

        boolean bConfirmField = confirmField != null && Boolean.valueOf( confirmField.getValue( ) );

        String strValueEntryConfirmField = null;

        if ( bConfirmField )
        {
            strValueEntryConfirmField = request.getParameter( PREFIX_ATTRIBUTE + entry.getIdEntry( ) + SUFFIX_CONFIRM_FIELD ).trim( );
        }

        List<RegularExpression> listRegularExpression = entry.getFieldByCode( FIELD_TEXT_CONF ).getRegularExpressionList( );
        Response responseGuid = new Response( );
        responseGuid.setEntry( entry );
        //response.setResponseValue( strValueEntry );
        responseGuid.setResponseValue( strValueCuid );

        if ( StringUtils.isNotBlank( responseGuid.getResponseValue( ) ) )
        {
        	responseGuid.setToStringValueResponse( getResponseValueForRecap( entry, request, responseGuid, locale ) );
        }
        else
        {
        	responseGuid.setToStringValueResponse( StringUtils.EMPTY );
        }

        responseGuid.setIterationNumber( getResponseIterationValue( request ) );
        responseGuid.setSortOrder(0);
        GenericAttributesUtils.createOrUpdateField( entry, "cuid", "cuid", strValueCuid );

        listResponse.add( responseGuid );
    	
    	int nOrder = 1;
    	for (String strField : lstAttributes ) {
    		
    		if ( request.getParameter(strField) != null )
    		{
				String strValue = request.getParameter(strField).trim();
				
				Field field = GenericAttributesUtils.findFieldByTitleInTheList( strField, entry.getFields( ) );
				
				GenericAttributesUtils.createOrUpdateField( entry, field.getCode(), field.getTitle(), strValue );
	
				Response response = new Response();
				response.setEntry(entry);
				response.setResponseValue(strValue);
				response.setToStringValueResponse(getResponseValueForRecap(entry, request, response, locale));
				response.setIterationNumber(getResponseIterationValue(request));
				response.setSortOrder(nOrder);
				response.setField(field);
				nOrder++;
				listResponse.add(response);
    		}
    	}
        
        

        return checkErrors( entry, confirmField, strValueCuid, strValueEntryConfirmField, listRegularExpression, bConfirmField, locale );
    }

    private GenericAttributeError checkErrors( Entry entry, Field confirmField, String strValueEntry, String strValueEntryConfirmField,
            List<RegularExpression> listRegularExpression, boolean bConfirmField, Locale locale )
    {
        // Checks if the entry value contains XSS characters
        if ( StringUtil.containsXssCharacters( strValueEntry ) )
        {
            GenericAttributeError error = new GenericAttributeError( );
            error.setMandatoryError( false );
            error.setTitleQuestion( entry.getTitle( ) );
            error.setErrorMessage( I18nService.getLocalizedString( MESSAGE_XSS_FIELD, locale ) );

            return error;
        }

        if ( entry.isMandatory( ) && StringUtils.isBlank( strValueEntry ) )
        {
            if ( StringUtils.isNotEmpty( entry.getErrorMessage( ) ) )
            {
                GenericAttributeError error = new GenericAttributeError( );
                error.setMandatoryError( true );
                error.setErrorMessage( entry.getErrorMessage( ) );

                return error;
            }

            return new MandatoryError( entry, locale );
        }

        if ( ( !strValueEntry.equals( StringUtils.EMPTY ) ) && CollectionUtils.isNotEmpty( listRegularExpression )
                && RegularExpressionService.getInstance( ).isAvailable( ) )
        {
            for ( RegularExpression regularExpression : listRegularExpression )
            {
                if ( !RegularExpressionService.getInstance( ).isMatches( strValueEntry, regularExpression ) )
                {
                    GenericAttributeError error = new GenericAttributeError( );
                    error.setMandatoryError( false );
                    error.setTitleQuestion( entry.getTitle( ) );
                    error.setErrorMessage( regularExpression.getErrorMessage( ) );

                    return error;
                }
            }
        }

        if ( bConfirmField && ( ( strValueEntryConfirmField == null ) || !strValueEntry.equals( strValueEntryConfirmField ) ) )
        {
            GenericAttributeError error = new GenericAttributeError( );
            error.setMandatoryError( false );
            error.setTitleQuestion( confirmField.getTitle( ) );
            error.setErrorMessage( I18nService.getLocalizedString( MESSAGE_CONFIRM_FIELD, new String [ ] {
                    entry.getTitle( )
            }, locale ) );

            return error;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResponseValueForExport( Entry entry, HttpServletRequest request, Response response, Locale locale )
    {
        return response.getResponseValue( );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getResponseValueForRecap( Entry entry, HttpServletRequest request, Response response, Locale locale )
    {
        return response.getResponseValue( );
    }
    
    /**
     * Builds the {@link ReferenceList} of all attributes available in the identity store
     * 
     * @return the {@link ReferenceList}
     */
    public ReferenceList getIdentityAttributesRefList( )
    {
    	RequestAuthor author = new RequestAuthor( );
        author.setType( AuthorType.admin );
        author.setName( AppPropertiesService.getProperty( PROPERTY_CLIENT_CODE) );
    	
        
        //GenericAttributesUtils.findFieldByIdInTheList( nIdField, getSqlQueryFields( entry ) );
//        try {
//			List<AttributeDto> lstAttributes = IdentityPickerService.getInstance().getIdentity(BEAN_REFERENTIAL_SERVICE, null).getAttributes();
//			List<AttributeDefinitionDto> lstAttributeDefinitions = IdentityPickerService.getInstance().getRules(null).getContract().getAttributeDefinitions();
//		} catch (IdentityStoreException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
    	_referentialService = SpringContextService.getBean( BEAN_REFERENTIAL_SERVICE );
    	AttributeSearchResponse attributeKeyList = null;
    	ReferenceList lstAttributes = new ReferenceList();
    	
    	try {
			attributeKeyList = _referentialService.getAttributeKeyList( AppPropertiesService.getProperty( PROPERTY_CLIENT_CODE), author );
		} catch (IdentityStoreException e) {
			AppLogService.error(e);
		}
    	
    	List<AttributeKeyDto> attributeKeys = attributeKeyList.getAttributeKeys();
        
        for (AttributeKeyDto attr : attributeKeys)
		{
			lstAttributes.addItem( attr.getKeyName(), attr.getName( ));
			
		}
    	
        return lstAttributes;
    }
}